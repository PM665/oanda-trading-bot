package org.pminin.oanda.bot.util;

import static com.oanda.v20.primitives.Direction.SHORT;
import static java.util.Comparator.comparingLong;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickData;
import com.oanda.v20.pricing_common.PriceValue;
import com.oanda.v20.primitives.DateTime;
import com.oanda.v20.primitives.Direction;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

public class TechAnalysisUtils {

    private static final double SCALING_CONSTANT = 1 / 0.015;
    private static final int STOCHASTIC_SLOWING = 3;
    private static final int STOCHASTIC_D_PERIOD = 3;

    private TechAnalysisUtils() {
    }

    public static double cci(Direction direction, List<Candlestick> candles, int maPeriod, int shift) {
        List<Candlestick> candlesSorted = sortCandles(candles);
        List<Double> prices = candlesSorted.stream().map(candle -> typicalPrice(candle, direction))
                .collect(toList());
        List<Double> smaList = sma(prices, maPeriod);
        double typicalPrice = prices.stream().findFirst().orElse(0.);
        double sma = smaList.stream().findFirst().orElse(0.);
        double mad = IntStream.range(1, maPeriod + 1)
                .mapToDouble(i -> deviation(prices, smaList, i + shift)).average().orElse(0);
        return SCALING_CONSTANT * (typicalPrice - sma) / mad;
    }

    public static double rsi(Direction direction, List<Candlestick> candles, int maPeriod, int shift) {
        List<Candlestick> candlesSorted = sortCandles(candles);
        List<Double> uValues = candlesSorted.stream().map(candle -> candleStickData(candle, direction))
                .map(data -> data.getC().doubleValue() - data.getO().doubleValue())
                .map(value -> value > 0 ? value : 0).collect(toList());
        List<Double> dValues = candlesSorted.stream().map(candle -> candleStickData(candle, direction))
                .map(data -> data.getO().doubleValue() - data.getC().doubleValue())
                .map(value -> value > 0 ? value : 0).collect(toList());
        double uEMA = ema(uValues, maPeriod, 1 + shift);
        double dEMA = ema(dValues, maPeriod, 1 + shift);
        double rs = uEMA / dEMA;
        return 100 - 100 / (1 + rs);
    }

    public static double sma(Direction direction, List<Candlestick> candles, int maPeriod, int shift) {
        List<Candlestick> candlesSorted = sortCandles(candles);
        List<Double> prices = candlesSorted.stream().map(candle -> closePrice(candle, direction))
                .collect(toList());
        return sma(prices, maPeriod, 1 + shift);
    }

    public static double stochasticK(Direction direction, List<Candlestick> candles, int maPeriod, int shift) {
        List<Candlestick> candlesSorted = candles.stream()
                .sorted(comparingLong(candle -> parseDateTime(((Candlestick) candle).getTime())).reversed())
                .skip(shift)
                .collect(toList());
        if (!candlesSorted.isEmpty()) {
            List<Double> lowPrices = candlesSorted.stream().limit((long) maPeriod + STOCHASTIC_SLOWING)
                    .map(candle -> lowPrice(candle, direction))
                    .collect(toList());
            List<Double> highPrices = candlesSorted.stream().limit((long) maPeriod + STOCHASTIC_SLOWING)
                    .map(candle -> highPrice(candle, direction))
                    .collect(toList());
            List<Double> closePrices = candlesSorted.stream().limit(STOCHASTIC_SLOWING)
                    .map(candle -> closePrice(candle, direction))
                    .collect(toList());
            double sum1 = IntStream.range(0, STOCHASTIC_SLOWING - 1)
                    .mapToDouble(i -> closePrices.get(i) - localMin(lowPrices, maPeriod, i)).sum();
            double sum2 = IntStream.range(0, STOCHASTIC_SLOWING - 1)
                    .mapToDouble(i -> localMax(highPrices, maPeriod, i) - localMin(lowPrices, maPeriod, i)).sum();
            return 100 * sum1 / sum2;
        } else {
            return 0;
        }
    }

    public static double ema(Direction direction, List<Candlestick> candles, int maPeriod, int shift) {
        List<Candlestick> candlesSorted = sortCandles(candles);
        List<Double> prices = candlesSorted.stream().map(candle -> closePrice(candle, direction))
                .collect(toList());
        return ema(prices, maPeriod, 1 + shift);
    }

    private static double ema(List<Double> values, int maPeriod, int shift) {
        if (maPeriod == shift) {
            return sma(values, maPeriod, shift);
        } else {
            double value = values.stream().skip(shift - 1L).findFirst().orElse(0.);
            double alpha = alpha(maPeriod);
            double emaPrev = ema(values, maPeriod, shift + 1);
            return alpha * value + (1 - alpha) * emaPrev;
        }
    }

    public static double deviation(Direction direction, List<Candlestick> candles, double ma, int maPeriod,
            int shift) {
        List<Candlestick> candlesSorted = sortCandles(candles);
        List<Double> prices = candlesSorted.stream().map(candle -> closePrice(candle, direction))
                .collect(toList());
        return deviation(prices, maPeriod, 1 + shift, ma);
    }

    public static double stochasticD(Direction direction, List<Candlestick> candles, int maPeriod, int shift) {
        List<Candlestick> candlesSorted = sortCandles(candles).stream()
                .skip(shift)
                .collect(toList());
        if (!candlesSorted.isEmpty()) {
            return IntStream.range(0, STOCHASTIC_D_PERIOD - 1)
                    .mapToDouble(i -> stochasticK(direction, candlesSorted, maPeriod, shift + i))
                    .average().orElse(0.);
        } else {
            return 0;
        }
    }

    public static List<Candlestick> sortCandles(List<Candlestick> candles) {
        return candles.stream()
                .sorted(comparingLong(candle -> parseDateTime(((Candlestick) candle).getTime())).reversed())
                .collect(toList());
    }

    private static double localMax(List<Double> values, int period, int shift) {
        return values.stream().skip(shift).limit(period).max(Double::compareTo).orElse(0.);
    }

    private static double localMin(List<Double> values, int period, int shift) {
        return values.stream().skip(shift).limit(period).min(Double::compareTo).orElse(0.);
    }

    private static List<Double> sma(List<Double> dValues, int maPeriod) {
        return IntStream.range(1, maPeriod + 1).mapToDouble(i -> sma(dValues, maPeriod, i)).boxed()
                .collect(toList());
    }

    private static double sma(List<Double> values, int maPeriod, int shift) {
        return values.stream().skip(shift - 1L).limit(maPeriod).mapToDouble(Double::doubleValue).average()
                .orElse(0);
    }

    static double typicalPrice(Candlestick candleStick, Direction direction) {
        return Optional.ofNullable(candleStick).map(candle -> candleStickData(candle, direction))
                .map(cd -> (cd.getH().doubleValue() + cd.getL().doubleValue() + cd.getC().doubleValue()) / 3)
                .orElse(0.);
    }

    private static double closePrice(Candlestick candleStick, Direction direction) {
        return mapToPrice(candleStick, direction, CandlestickData::getC);
    }

    private static double highPrice(Candlestick candleStick, Direction direction) {
        return mapToPrice(candleStick, direction, CandlestickData::getH);
    }

    private static double lowPrice(Candlestick candleStick, Direction direction) {
        return mapToPrice(candleStick, direction, CandlestickData::getL);
    }

    private static Double mapToPrice(Candlestick candleStick, Direction direction,
            Function<CandlestickData, PriceValue> getPrice) {
        return Optional.ofNullable(candleStick).map(candle -> candleStickData(candle, direction))
                .map(getPrice)
                .map(PriceValue::doubleValue)
                .orElse(0.);
    }

    private static double deviation(List<Double> prices, List<Double> smaList, int index) {
        double pt = prices.stream().skip(index - 1L).findFirst().orElse(1.);
        double sma = smaList.stream().skip(index - 1L).findFirst().orElse(1.);
        return Math.abs(pt - sma);
    }

    private static double deviation(List<Double> prices, int maPeriod, int index, double avg) {
        return Math.sqrt(prices.stream().skip(index - 1L).limit(maPeriod)
                .mapToDouble(price -> Math.pow(price - avg, 2))
                .sum() / maPeriod);
    }

    private static double alpha(int n) {
        return 2. / (n + 1);
    }

    private static CandlestickData candleStickData(Candlestick candle, Direction direction) {
        return SHORT == direction ? candle.getBid() : candle.getAsk();
    }

    public static long parseDateTime(DateTime dateTime) {
        return MILLISECONDS.convert(Math.round(Double.valueOf(dateTime.toString())), SECONDS);
    }

    public static DateTime toDateTime(Date date) {
        return new DateTime(String.valueOf(SECONDS.convert(date.getTime(), MILLISECONDS)));
    }
}

