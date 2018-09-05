package org.pminin.oanda.bot.util;

import static com.oanda.v20.primitives.Direction.LONG;
import static org.pminin.oanda.bot.util.TechAnalysisUtils.deviation;
import static org.pminin.oanda.bot.util.TechAnalysisUtils.sma;

import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickData;
import com.oanda.v20.pricing_common.PriceValue;
import com.oanda.v20.primitives.Direction;
import com.oanda.v20.primitives.Instrument;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.pminin.oanda.bot.config.BotProperties.StrategyDefinition;
import org.pminin.oanda.bot.model.AccountException;
import org.pminin.oanda.bot.model.StrategyContext;
import org.pminin.oanda.bot.model.StrategyContext.Bollinger;
import org.pminin.oanda.bot.model.StrategyContext.Candle;
import org.pminin.oanda.bot.model.StrategyContext.Candle.CandleBuilder;
import org.pminin.oanda.bot.services.AccountService;

@Slf4j
public class StrategyContextUtils {

    private StrategyContextUtils() {
    }

    public static StrategyContext createContext(AccountService accountService, String accountId,
            List<Candlestick> candles, Instrument instrument, StrategyDefinition definition,
            Date recentTradeDate) throws AccountException {

        Bollinger bollinger = bollinger(candles, definition);
        Bollinger prevBollinger = bollinger(candles, definition);
        double currentPrice = currentPrice(candles, definition.getDirection());
        Candle prevCandle = previousCandle(candles, definition.getDirection());

        return StrategyContext.builder()
                .bollinger(bollinger)
                .previousBollinger(prevBollinger)
                .currentPrice(currentPrice)
                .previousCandle(prevCandle)
                .pip(pip(instrument))
                .nav(nav(accountService, accountId))
                .recentOrderTime(recentTradeDate)
                .build();
    }

    private static double nav(AccountService accountService, String accountId) throws AccountException {
        return accountService.getAccount(accountId).getNAV().doubleValue();
    }

    private static Candle previousCandle(List<Candlestick> candles, Direction direction) {
        CandleBuilder builder = Candle.builder();
        Optional<Candlestick> candleOptional = candles.stream().skip(1).findFirst();
        candleOptional
                .map(candle -> direction == LONG ? candle.getAsk() : candle.getBid())
                .ifPresent(data -> builder
                        .close(data.getC().doubleValue())
                        .open(data.getO().doubleValue())
                        .high(data.getH().doubleValue())
                        .low(data.getL().doubleValue()));
        candleOptional
                .map(Candlestick::getTime)
                .map(TechAnalysisUtils::parseDateTime)
                .map(Date::new)
                .ifPresent(builder::time);
        return builder.build();
    }

    private static double currentPrice(List<Candlestick> candles, Direction direction) {
        return candles.stream().findFirst()
                .map(candle -> direction == LONG ? candle.getAsk() : candle.getBid())
                .map(CandlestickData::getC).map(PriceValue::doubleValue).orElse(0.);
    }

    private static Bollinger bollinger(List<Candlestick> candles, StrategyDefinition definition) {
        double sma = sma(definition.getDirection(), candles, definition.getMaPeriod(), definition.getShift());
        double deviation = deviation(definition.getDirection(), candles, sma, definition.getMaPeriod(),
                definition.getShift());
        double lower = sma - 2 * deviation;
        double upper = sma + 2 * deviation;
        return Bollinger.builder()
                .sma(sma)
                .lower(lower)
                .upper(upper)
                .width((upper - lower))
                .build();
    }

    private static double pip(Instrument instrument) {
        return Math.pow(10, instrument.getPipLocation());
    }
}
