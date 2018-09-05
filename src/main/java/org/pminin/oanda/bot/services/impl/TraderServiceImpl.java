package org.pminin.oanda.bot.services.impl;

import static com.oanda.v20.primitives.Direction.SHORT;

import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.primitives.Direction;
import com.oanda.v20.primitives.Instrument;
import com.oanda.v20.trade.TradeID;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.pminin.oanda.bot.model.AccountException;
import org.pminin.oanda.bot.services.AccountService;
import org.pminin.oanda.bot.services.StrategyService;
import org.pminin.oanda.bot.services.TraderService;

@Slf4j
public class TraderServiceImpl implements TraderService {

    private final AccountService accountService;
    @NonNull
    private final StrategyService strategyService;
    @Getter
    private final Instrument instrument;
    @Getter
    private final String accountId;
    private Date recentTradeDate = new Date(0);

    public TraderServiceImpl(AccountService accountService, StrategyService strategyService, String instrument,
            String accountId) throws AccountException {
        this.accountService = accountService;
        this.strategyService = strategyService;
        this.accountId = accountId;
        this.instrument = accountService.getInstrument(instrument, accountId);
    }

    @PostConstruct
    public void postConstruct() {
        log.info("Created trader bean for account {}, {}, {}", accountService.getName(),
                strategyService.getName(), instrument.getName());
    }

    @Override
    public CandlestickGranularity getGranularity() {
        return strategyService.getGranularity();
    }

    @Override
    public void processCandles(List<Candlestick> candles) {
        log.debug("Processing candles ({}, {})...", instrument.getDisplayName(), strategyService.getName());
        processOpenTriggers(candles);
    }


    private void processOpenTriggers(List<Candlestick> candles) {
        try {
            if (strategyService.checkOpenTrigger(candles, accountId, instrument, recentTradeDate)) {
                log.info("Context for {} {}: \n{}", accountId, instrument, strategyService.getContext());

                Direction direction = strategyService.direction();
                log.info("Creating {} order  ({})", direction, instrument.getName());

                long maxTradesOpen = strategyService.getMaxTradesOpen();
                Long openTradeCount = accountService.getAccount(accountId).getOpenTradeCount();
                double unitsAvailable = accountService.accountUnitsAvailable(accountId, instrument, direction);
                long tradesAvailable = maxTradesOpen - openTradeCount;
                if (tradesAvailable > 0) {
                    double tradeAmount = unitsAvailable / tradesAvailable;
                    if (direction == SHORT) {
                        tradeAmount *= -1;
                    }

                    double takeProfit = strategyService.takeProfit();
                    double stopLoss = strategyService.stopLoss();
                    TradeID newOrder = accountService
                            .createOrder(accountId, instrument, tradeAmount, takeProfit,
                                    stopLoss);
                    recentTradeDate = new Date();
                    log.info("Created order {}", newOrder);
                } else {
                    log.info("Maximum number of trades were opened ({})", maxTradesOpen);
                }
            }
        } catch (AccountException e) {
            log.error("Could not check trigger for opening trade due to an error", e);
        }
    }

}
