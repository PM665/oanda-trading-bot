package org.pminin.oanda.bot.model;

import java.util.Date;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class StrategyContext {

    @NonNull
    private Candle previousCandle;
    @NonNull
    private Bollinger previousBollinger;
    @NonNull
    private Bollinger bollinger;
    private Date recentOrderTime;
    private double pip;
    private double currentPrice;

    @Value
    @Builder
    public static class Candle {

        private Date time;
        private double open;
        private double close;
        private double low;
        private double high;
    }

    @Value
    @Builder
    public static class Bollinger {

        private double sma;
        private double width;
        private double upper;
        private double lower;
    }
}
