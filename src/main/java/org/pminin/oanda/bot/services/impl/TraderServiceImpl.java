package org.pminin.oanda.bot.services.impl;

import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.primitives.Instrument;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
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
            String accountId) {
        this.accountService = accountService;
        this.strategyService = strategyService;
        this.accountId = accountId;
        this.instrument = accountService.getInstrument(instrument);
    }

    @PostConstruct
    public void postConstruct() {
        log.info("Created trader bean for account {}, strategyService {}, {}", accountService.getName(),
                strategyService.getName(), instrument.getName());
    }

    @Override
    public CandlestickGranularity getGranularity() {
        return strategyService.getGranularity();
    }

    @Override
    public void processCandles(List<Candlestick> candles) {
        log.info("Processing candles ({})...", instrument.getDisplayName());
        processOpenTriggers(candles);
    }


    private void processOpenTriggers(List<Candlestick> candles) {
        if (strategyService.checkOpenTrigger(candles)) {
            try {
                log.info("Creating {} order  ({})", strategyService.direction(), instrument.getName());
                accountService.createOrder(instrument, strategyService.tradeAmount(), strategyService.takeProfit(),
                        strategyService.stopLoss());
            } catch (Exception e) {
                log.error("Cannot create order with unexpected exception ({})", instrument.getDisplayName(), e);
            }
        }
    }

}
