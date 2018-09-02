package org.pminin.oanda.bot.services.impl;

import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.primitives.Instrument;
import com.oanda.v20.trade.TradeID;
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
        log.info("Processing candles ({}, {})...", instrument.getDisplayName(), strategyService.getName());
        processOpenTriggers(candles);
    }


    private void processOpenTriggers(List<Candlestick> candles) {
        try {
            if (strategyService.checkOpenTrigger(candles, accountId, instrument)) {
                log.info("Creating {} order  ({})", strategyService.direction(), instrument.getName());

                double tradeAmount =
                        accountService.unitPrice(accountId, instrument, strategyService.direction()) * strategyService
                                .tradeAmount();
                double unitsAvailable = accountService
                        .accountUnitsAvailable(accountId, instrument, strategyService.direction());
                if (tradeAmount <= unitsAvailable) {
                    TradeID newOrder = accountService
                            .createOrder(accountId, instrument, tradeAmount, strategyService.takeProfit(),
                                    strategyService.stopLoss());
                    log.info("Created order {}", newOrder);
                } else {
                    log.info("Not enough units to open a trade ({} > {})", tradeAmount, unitsAvailable);
                }
            }
        } catch (AccountException e) {
            log.error("Could not check trigger for opening trade due to an error", e);
        }
    }

}
