package org.pminin.oanda.bot.model;

public class AccountException extends Exception {

    public AccountException(String message) {
        super(message);
    }

    public AccountException(String message, Throwable cause) {
        super(message, cause);
    }
}
