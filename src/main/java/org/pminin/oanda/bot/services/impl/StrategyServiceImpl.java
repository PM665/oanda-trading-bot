package org.pminin.oanda.bot.services.impl;

import static org.pminin.oanda.bot.util.StrategyContextUtils.createContext;

import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.primitives.Direction;
import com.oanda.v20.primitives.Instrument;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.pminin.oanda.bot.config.BotProperties.StrategyDefinition;
import org.pminin.oanda.bot.model.AccountException;
import org.pminin.oanda.bot.model.StrategyContext;
import org.pminin.oanda.bot.services.AccountService;
import org.pminin.oanda.bot.services.StrategyService;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

@Slf4j
public class StrategyServiceImpl implements StrategyService {

    private final AccountService accountService;
    private StrategyDefinition definition;
    private StrategyContext context;

    public StrategyServiceImpl(StrategyDefinition strategyDefinition,
            AccountService accountService) {
        definition = strategyDefinition;
        this.accountService = accountService;
    }

    @PostConstruct
    public void postConstruct() {
        log.info("Created strategy bean {}", definition.getName());
    }

    @Override
    public String getName() {
        return definition.getName();
    }

    @Override
    public CandlestickGranularity getGranularity() {
        return definition.getGranularity();
    }

    @Override
    public boolean checkOpenTrigger(List<Candlestick> candles, String accountId,
            Instrument instrument) throws AccountException {
        context = createContext(accountService, accountId, candles, instrument, definition);
        log.info( "Context for {} {}: \n{}", accountId, instrument, context);
        return definition.getTrigger().stream()
                .map(trigger -> parseExpression(trigger, Boolean.class))
                .reduce(true, (a, b) -> a && b);
    }

    @Override
    public double takeProfit() {
        return parseExpression(definition.getTakeProfit(), Double.class);
    }

    @Override
    public double stopLoss() {
        return parseExpression(definition.getStopLoss(), Double.class);
    }

    @Override
    public double tradeAmount() {
        return parseExpression(definition.getTradeAmount(), Double.class);
    }

    @Override
    public Direction direction() {
        return definition.getDirection();
    }


    private <T> T parseExpression(String expression, Class<T> aClass) {
        ExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression(expression);
        return exp.getValue(context, aClass);
    }


}
