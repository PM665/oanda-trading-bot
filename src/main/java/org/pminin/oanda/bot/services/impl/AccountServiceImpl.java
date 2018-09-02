package org.pminin.oanda.bot.services.impl;

import static com.oanda.v20.primitives.Direction.LONG;
import static com.oanda.v20.primitives.Direction.SHORT;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import com.oanda.v20.Context;
import com.oanda.v20.ExecuteException;
import com.oanda.v20.RequestException;
import com.oanda.v20.account.Account;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.account.AccountInstrumentsResponse;
import com.oanda.v20.account.AccountListResponse;
import com.oanda.v20.account.AccountProperties;
import com.oanda.v20.order.MarketOrderRequest;
import com.oanda.v20.order.OrderCreateRequest;
import com.oanda.v20.order.OrderCreateResponse;
import com.oanda.v20.pricing.ClientPrice;
import com.oanda.v20.primitives.AcceptDatetimeFormat;
import com.oanda.v20.primitives.Direction;
import com.oanda.v20.primitives.Instrument;
import com.oanda.v20.trade.TradeID;
import com.oanda.v20.trade.TradeSummary;
import com.oanda.v20.transaction.OrderFillTransaction;
import com.oanda.v20.transaction.StopLossDetails;
import com.oanda.v20.transaction.TakeProfitDetails;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.HttpClients;
import org.pminin.oanda.bot.config.BotProperties.AccountDefinition;
import org.pminin.oanda.bot.model.AccountException;
import org.pminin.oanda.bot.services.AccountService;
import org.pminin.oanda.bot.util.TechAnalysisUtils;

@Slf4j
@Value
public class AccountServiceImpl implements AccountService {

    private final Context ctx;
    private final String name;
    private final Map<String, AccountID> accounts = new HashMap<>();
    private final Map<AccountID, List<Instrument>> instrumentsMap = new HashMap<>();

    private final String mainAccountId;

    public AccountServiceImpl(AccountDefinition accountDefinition) {
        name = accountDefinition.getName();
        ctx = new Context(accountDefinition.getUrl(), accountDefinition.getToken(), "TradingAdvisor_" + name,
                AcceptDatetimeFormat.UNIX, HttpClients.createDefault());
        mainAccountId = accountDefinition.getAccountId();
    }

    @Override
    public Instrument getInstrument(String instrumentStr) throws AccountException {
        return getInstrument(instrumentStr, mainAccountId);
    }

        @Override
    public Instrument getInstrument(String instrumentStr, String accountId) throws AccountException {
        AccountID accountID = getAccountID(accountId);
        return instrumentsMap.getOrDefault(accountID, emptyList()).stream()
                .filter(ins -> ins.getName().toString().equals(instrumentStr))
                .findFirst().orElse(null);
    }

    @Override
    public TradeID createOrder(String accountId, Instrument instrument, double units, double tpPrice, double slPrice)
            throws AccountException {
        try {
            log.info("Place a Market Order: {}[{} units] TP@{} SL@{}", instrument, units, tpPrice, slPrice);
            // Create the new request
            AccountID accountID = getAccountID(accountId);
            OrderCreateRequest request = new OrderCreateRequest(accountID);
            TakeProfitDetails tp = new TakeProfitDetails()
                    .setPrice(tpPrice);
            StopLossDetails sl = new StopLossDetails()
                    .setPrice(slPrice);
            MarketOrderRequest marketorderrequest = new MarketOrderRequest()
                    .setInstrument(instrument.getName())
                    .setUnits(units)
                    .setStopLossOnFill(sl)
                    .setTakeProfitOnFill(tp);
            request.setOrder(marketorderrequest);
            // Execute the request and obtain the response object
            OrderCreateResponse response = ctx.order.create(request);
            // Extract the Order Fill transaction for the executed Market Order
            OrderFillTransaction transaction = response.getOrderFillTransaction();
            // Extract the trade ID of the created trade from the transaction and keep it for future action
            return transaction.getTradeOpened().getTradeID();
        } catch (ExecuteException | RequestException e) {
            throw new AccountException("Cannot create order", e);
        }
    }

    @Override
    public double accountUnitsAvailable(String accountId, Instrument instrument, Direction direction)
            throws AccountException {
        double available = getAccount(accountId).getMarginAvailable().doubleValue();
        return available * unitPrice(accountId, instrument, direction);
    }

    @Override
    public double unitPrice(String accountId, Instrument instrument, Direction direction) throws AccountException {
        try {        AccountID accountID = getAccountID(accountId);

            return ctx.pricing.get(accountID, singletonList(instrument.getName()))
                    .getPrices().stream()
                    .filter(price -> price.getInstrument().equals(instrument.getName()))
                    .findFirst()
                    .map(ClientPrice::getQuoteHomeConversionFactors)
                    .map(factor ->
                            direction == LONG ?
                                    factor.getPositiveUnits().doubleValue() :
                                    factor.getNegativeUnits().doubleValue())
                    .orElse(0.);
        } catch (ExecuteException | RequestException e) {
            throw new AccountException("Cannot calculate unit price", e);
        }
    }

    @Override
    public Date recentOrderTime(String accountId, Instrument instrument, Direction direction) throws AccountException {
        return getAccount(accountId).getTrades().stream()
                .filter(trade -> trade.getInstrument().equals(instrument.getName()))
                .filter(trade -> direction == SHORT && trade.getInitialUnits().doubleValue() < 0)
                .map(TradeSummary::getOpenTime)
                .map(TechAnalysisUtils::parseDateTime)
                .max(Long::compareTo)
                .map(Date::new)
                .orElse(new Date(0));

    }

    @Override
    public String toString() {
        return "OandaAccount{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public Account getAccount(String accountId) throws AccountException {
        try {
            AccountID accountID = getAccountID(accountId);
            return ctx.account.get(accountID).getAccount();
        } catch (ExecuteException | RequestException e) {
            throw new AccountException("Cannot get account info", e);
        }
    }

    private AccountID getAccountID(String accountId) throws AccountException {
        AccountID accountID = accounts.get(accountId);
        if (accountID == null) {
            try {
                log.info("Make sure we have a valid account {}", accountId);
                AccountListResponse response = ctx.account.list();
                List<AccountProperties> accountProperties = response.getAccounts();
                accountID = accountProperties.stream()
                        .map(AccountProperties::getId)
                        .filter(id -> id.toString().equals(accountId))
                        .findFirst().orElseThrow(() ->
                                new AccountException("Account " + accountId + " not found"));
                log.info("Make sure the account has a non zero balance");
                Account account = ctx.account.get(accountID).getAccount();
                if (account.getBalance().doubleValue() <= 0.0) {
                    throw new AccountException("Account " + accountId + " balance " + account.getBalance() + " <= 0");
                }
                log.info("Make sure the account has given instrument");
                AccountInstrumentsResponse instrumentsResponse = ctx.account.instruments(accountID);
                accounts.put(accountId, accountID);
                instrumentsMap.put(accountID, instrumentsResponse.getInstruments());
            } catch (ExecuteException | RequestException e) {
                throw new AccountException("Cannot initialize account " + accountId, e);
            }
        }
        return accountID;
    }

}
