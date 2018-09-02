package org.pminin.oanda.bot.services;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.oanda.v20.instrument.CandlestickGranularity;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.pminin.oanda.bot.config.BotProperties;
import org.pminin.oanda.bot.config.BotProperties.StrategyDefinition;
import org.pminin.oanda.bot.config.BotProperties.TraderOptions;
import org.pminin.oanda.bot.services.impl.CollectorServiceImpl;
import org.pminin.oanda.bot.services.impl.StrategyServiceImpl;
import org.pminin.oanda.bot.services.impl.TraderServiceImpl;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;

@Slf4j
public class ServiceRegistrar {

    private static final String ACCOUNT_SERVICE_BEAN_ID = "accountService";
    private static final String STRATEGY_SERVICE_BEAN_PREFIX = "strategy-";
    private static final String TRADER_SERVICE_BEAN_PREFIX = "trader-";
    private static final String COLLECTOR_SERVICE_BEAN_PREFIX = "collector";
    private final Map<String, Boolean> strategyEnabledMap;
    private ApplicationContext applicationContext;

    public ServiceRegistrar(BotProperties botProperties, ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        botProperties.getStrategies().stream()
                .filter(StrategyDefinition::isEnabled)
                .forEach(this::registerStrategyBean);
        strategyEnabledMap = botProperties.getStrategies().stream()
                .collect(toMap(StrategyDefinition::getName, StrategyDefinition::isEnabled));
        createCollectors(botProperties);
        botProperties.getTraderOptions().forEach(this::registerTraders);
    }

    private void registerTraders(TraderOptions traderOptions) {
        traderOptions.getTraders().forEach(
                (instrument, strategies) -> registerTraders(traderOptions.getAccountId(), instrument, strategies));
    }

    private void registerTraders(String accountId, String instrument, Set<String> strategies) {
        strategies.stream()
                .filter(key -> strategyEnabledMap.getOrDefault(key, false))
                .forEach(strategy -> registerTraderService(accountId, instrument, strategy));
    }

    private void registerTraderService(String accountId, String instrument, String strategy) {
        BeanDefinitionBuilder b = BeanDefinitionBuilder.rootBeanDefinition(TraderServiceImpl.class)
                .addConstructorArgReference(ACCOUNT_SERVICE_BEAN_ID)
                .addConstructorArgReference(STRATEGY_SERVICE_BEAN_PREFIX + strategy)
                .addConstructorArgValue(instrument)
                .addConstructorArgValue(accountId);
        registerBean(String.format("%s%s-%s", TRADER_SERVICE_BEAN_PREFIX, strategy, instrument),
                b.getBeanDefinition());
    }

    private void createCollectors(BotProperties botProperties) {
        Map<String, CandlestickGranularity> granularities = botProperties.getStrategies().stream()
                .filter(StrategyDefinition::isEnabled)
                .collect(toMap(StrategyDefinition::getName, StrategyDefinition::getGranularity));
        Map<String, Set<CandlestickGranularity>> instruments = botProperties.getTraderOptions().stream()
                .map(TraderOptions::getTraders)
                .map(Map::keySet)
                .flatMap(Set::stream)
                .collect(toMap(identity(), s -> new HashSet<>()));
        botProperties.getTraderOptions().stream()
                .map(TraderOptions::getTraders)
                .forEach(map -> map.forEach((instrument, strategies) -> strategies.stream()
                        .filter(key -> strategyEnabledMap.getOrDefault(key, false))
                        .map(granularities::get)
                        .forEach(instruments.get(instrument)::add)));
        instruments.forEach(this::registerCollectorBeans);
    }

    private void registerStrategyBean(StrategyDefinition strategyDefinition) {
        log.info("Registering Strategy bean: {}", strategyDefinition);
        registerStrategyService(strategyDefinition);
    }

    private void registerStrategyService(StrategyDefinition strategyDefinition) {
        BeanDefinitionBuilder b = BeanDefinitionBuilder.rootBeanDefinition(StrategyServiceImpl.class)
                .addConstructorArgValue(strategyDefinition)
                .addConstructorArgReference(ACCOUNT_SERVICE_BEAN_ID);
        registerBean(STRATEGY_SERVICE_BEAN_PREFIX + strategyDefinition.getName(), b.getBeanDefinition());
    }

    private void registerCollectorBeans(String instrument, Set<CandlestickGranularity> granularities) {
        granularities.forEach(granularity -> registerCollectorBeans(instrument, granularity));
    }

    private void registerCollectorBeans(String instrument, CandlestickGranularity granularity) {
        log.info("Registering Collector beans with instrument: {}, granularity: {}", instrument, granularity);
        registerCollectorService(instrument, granularity);
    }

    private void registerCollectorService(String instrument, CandlestickGranularity granularity) {
        BeanDefinitionBuilder b = BeanDefinitionBuilder.rootBeanDefinition(CollectorServiceImpl.class)
                .addConstructorArgReference(ACCOUNT_SERVICE_BEAN_ID)
                .addConstructorArgValue(instrument)
                .addConstructorArgValue(granularity);
        registerBean(String.format("%s%s-%s", COLLECTOR_SERVICE_BEAN_PREFIX, instrument, granularity),
                b.getBeanDefinition());
    }

    private void registerBean(String beanId, BeanDefinition instance) {
        AutowireCapableBeanFactory factory =
                applicationContext.getAutowireCapableBeanFactory();
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) factory;
        registry.registerBeanDefinition(beanId, instance);
    }

}
