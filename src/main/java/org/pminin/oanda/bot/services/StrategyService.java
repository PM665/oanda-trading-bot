package org.pminin.oanda.bot.services;

import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.primitives.Direction;
import com.oanda.v20.primitives.Instrument;
import java.util.List;
import org.pminin.oanda.bot.model.AccountException;

public interface StrategyService {

    String getName();

    CandlestickGranularity getGranularity();

    boolean checkOpenTrigger(List<Candlestick> candles, String accountId,
            Instrument instrument) throws AccountException;

    double takeProfit();

    double stopLoss();

    Direction direction();

    double tradeAmount();
}
