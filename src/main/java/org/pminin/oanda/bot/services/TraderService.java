package org.pminin.oanda.bot.services;

import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.primitives.Instrument;
import java.util.List;

public interface TraderService {

    Instrument getInstrument();

    CandlestickGranularity getGranularity();

    void processCandles(List<Candlestick> candles);

}
