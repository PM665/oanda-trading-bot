package org.pminin.oanda.bot.model;

import java.util.Date;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StrategyContext {

    private Candle previousCandle;
    private Bollinger previousBollinger;
    private Bollinger bollinger;
    private Date recentOrderTime;
    private double pip;
    private double currentPrice;
    private double nav;

    @Value
    @Builder
    private class Candle {

        private double open;
        private double close;
        private double low;
        private double high;
    }

    @Value
    @Builder
    private class Bollinger {

        private double sma;
        private double width;
        private double upper;
        private double lower;
    }
}
