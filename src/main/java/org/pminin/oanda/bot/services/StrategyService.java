package org.pminin.oanda.bot.services;

import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.primitives.Direction;
import java.util.List;

public interface StrategyService {

    String getName();

    CandlestickGranularity getGranularity();

    boolean checkOpenTrigger(List<Candlestick> candles);

    double takeProfit();

    double stopLoss();

    Direction direction();

    double tradeAmount();
}
