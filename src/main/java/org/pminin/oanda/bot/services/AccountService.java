package org.pminin.oanda.bot.services;

import com.oanda.v20.Context;
import com.oanda.v20.account.Account;
import com.oanda.v20.primitives.Direction;
import com.oanda.v20.primitives.Instrument;
import com.oanda.v20.trade.TradeID;
import org.pminin.oanda.bot.model.AccountException;

public interface AccountService {

    String getName();

    Context getCtx();

    Instrument getInstrument(String instrumentStr);

    TradeID createOrder(Instrument instrument, double units, double tpPrice, double slPrice)
            throws AccountException;

    double accountUnitsAvailable(Direction direction, Instrument instrument)
            throws AccountException;

    double unitPrice(Direction direction, Instrument instrument) throws AccountException;

    Account getAccount() throws AccountException;
}
