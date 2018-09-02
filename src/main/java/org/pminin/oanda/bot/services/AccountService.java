package org.pminin.oanda.bot.services;

import com.oanda.v20.Context;
import com.oanda.v20.account.Account;
import com.oanda.v20.primitives.Direction;
import com.oanda.v20.primitives.Instrument;
import com.oanda.v20.trade.TradeID;
import java.util.Date;
import org.pminin.oanda.bot.model.AccountException;

public interface AccountService {

    String getName();

    Context getCtx();

    Instrument getInstrument(String instrumentStr, String accountId) throws AccountException;

    Instrument getInstrument(String instrumentStr) throws AccountException;

    TradeID createOrder(String accountId, Instrument instrument, double units, double tpPrice, double slPrice)
            throws AccountException;

    double accountUnitsAvailable(String accountId, Instrument instrument, Direction direction)
            throws AccountException;

    double unitPrice(String accountId, Instrument instrument, Direction direction) throws AccountException;

    Account getAccount(String accountId) throws AccountException;

    Date recentOrderTime(String accountId, Instrument instrument, Direction direction) throws AccountException;
}
