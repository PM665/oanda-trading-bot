package org.pminin.oanda.bot.services.impl;

import static java.util.Comparator.comparingLong;
import static org.pminin.oanda.bot.util.TechAnalysisUtils.parseDateTime;
import static org.pminin.oanda.bot.util.TechAnalysisUtils.sortCandles;
import static org.pminin.oanda.bot.util.TechAnalysisUtils.toDateTime;

import com.oanda.v20.ExecuteException;
import com.oanda.v20.RequestException;
import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.instrument.InstrumentCandlesRequest;
import com.oanda.v20.primitives.Instrument;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.pminin.oanda.bot.services.AccountService;
import org.pminin.oanda.bot.services.CollectorService;
import org.pminin.oanda.bot.services.TraderService;
import org.pminin.oanda.bot.util.TechAnalysisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public class CollectorServiceImpl implements CollectorService {

    private static final long MAX_HISTORY_LENGTH = 300;
    private final Instrument instrument;
    private final CandlestickGranularity granularity;
    private final List<Candlestick> candles = new ArrayList<>();
    private AccountService accountService;
    @Setter(onMethod_ = {@Autowired})
    private List<TraderService> traders;

    public CollectorServiceImpl(AccountService accountService, String instrument,
            CandlestickGranularity granularity) {
        this.accountService = accountService;
        this.instrument = accountService.getInstrument(instrument);
        this.granularity = granularity;
    }

    @PostConstruct
    public void postConstruct() {
        log.info("Created a collector bean with accountService '{}', instrument: {}, granularity: {}",
                accountService.getName(),
                instrument, granularity);
    }

    public void collectCandles() {
        try {
            InstrumentCandlesRequest req = getInstrumentCandlesRequest();
            List<Candlestick> newCandles = sortCandles(accountService.getCtx().instrument.candles(req).getCandles());
            addNewCandles(newCandles);
        } catch (RequestException | ExecuteException e) {
            log.error("Could not request candles", e);
        }
    }

    private void addNewCandles(List<Candlestick> newCandles) {
        long candlesMaxCompleteTime = getMaxCompleteTime(candles);
        log.debug("Max completed time: {}", new Date(candlesMaxCompleteTime));
        newCandles.removeIf(candlestick -> candlestick.getComplete()
                && parseDateTime(candlestick.getTime()) <= candlesMaxCompleteTime);
        log.debug("new Candles:");
        newCandles.stream()
                .map(Candlestick::getTime)
                .map(TechAnalysisUtils::parseDateTime)
                .map(Date::new)
                .map(Date::toString)
                .forEach(log::debug);
        candles.removeIf(candlestick -> parseDateTime(candlestick.getTime()) > candlesMaxCompleteTime);
        candles.addAll(0, sortCandles(newCandles));
        notifyTradersIfNecessary(newCandles);
    }

    private void notifyTradersIfNecessary(List<Candlestick> newCandles) {
        if (newCandles.stream().anyMatch(Candlestick::getComplete)) {
            log.debug("Notifying traders");
            traders.stream()
                    .filter(trader -> instrument.equals(trader.getInstrument()))
                    .filter(trader -> granularity.equals(trader.getGranularity()))
                    .forEach(trader -> trader.processCandles(candles));
        }
    }

    private InstrumentCandlesRequest getInstrumentCandlesRequest() {
        return candles.stream()
                .sorted(comparingLong(candle -> parseDateTime(((Candlestick) candle).getTime())).reversed())
                .filter(Candlestick::getComplete)
                .map(Candlestick::getTime)
                .max(Comparator.comparingLong(TechAnalysisUtils::parseDateTime))
                .map(time -> createRequest().setFrom(time)
                        .setIncludeFirst(true))
                .orElseGet(() -> createRequest().setCount(MAX_HISTORY_LENGTH));
    }

    private InstrumentCandlesRequest createRequest() {
        return new InstrumentCandlesRequest(instrument.getName())
                .setPrice("AB")
                .setGranularity(granularity)
                .setTo(toDateTime(new Date()));
    }

    private long getMaxCompleteTime(List<Candlestick> list) {
        return list.stream().filter(Candlestick::getComplete).map(Candlestick::getTime)
                .map(TechAnalysisUtils::parseDateTime).max(Long::compareTo).orElse(0L);
    }

    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public void cutCandles() {
        candles.stream()
                .sorted(comparingLong(candle -> parseDateTime(((Candlestick) candle).getTime())).reversed())
                .skip(MAX_HISTORY_LENGTH)
                .forEach(candles::remove);
    }
}
