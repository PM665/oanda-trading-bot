package org.pminin.oanda.bot.services.impl;

import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.primitives.Direction;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.pminin.oanda.bot.config.BotProperties.StrategyDefinition;
import org.pminin.oanda.bot.model.StrategyContext;
import org.pminin.oanda.bot.services.StrategyService;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

@Slf4j
public class StrategyServiceImpl implements StrategyService {

    private final String name;
    private final List<String> trigger;
    private final Direction direction;
    private final String takeProfit;
    private final String stopLoss;
    private final String tradeAmount;
    @Getter
    private final CandlestickGranularity granularity;

    public StrategyServiceImpl(StrategyDefinition strategyDefinition) {
        name = strategyDefinition.getName();
        this.trigger = strategyDefinition.getTrigger();
        this.direction = strategyDefinition.getDirection();
        this.takeProfit = strategyDefinition.getTakeProfit();
        this.stopLoss = strategyDefinition.getStopLoss();
        this.tradeAmount = strategyDefinition.getTradeAmount();
        this.granularity = strategyDefinition.getGranularity();
    }

    @PostConstruct
    public void postConstruct() {
        log.info("Created strategy bean {}", name);
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean checkOpenTrigger(List<Candlestick> candles) {
        // create context
        // check each expression
        // return true if all of them are true
        return false;
    }

    @Override
    public double takeProfit() {
        // create context
        // calculate the value
        return 0;
    }

    @Override
    public double stopLoss() {
        // create context
        // calculate the value
        return 0;
    }

    @Override
    public Direction direction() {
        return null;
    }

    @Override
    public double tradeAmount() {
        return 0;
    }


    private <T> T parseExpression(String expression, Class<T> aClass, StrategyContext context)  {
        ExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression(expression);
        return exp.getValue(context, aClass);
    }

}
