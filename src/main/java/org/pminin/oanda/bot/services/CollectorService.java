package org.pminin.oanda.bot.services;

public interface CollectorService {

    void collectCandles();
    void cutCandles();
}