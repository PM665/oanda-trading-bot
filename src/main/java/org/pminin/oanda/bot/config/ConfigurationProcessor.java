package org.pminin.oanda.bot.config;

import lombok.extern.slf4j.Slf4j;
import org.pminin.oanda.bot.model.AccountException;
import org.pminin.oanda.bot.services.AccountService;
import org.pminin.oanda.bot.services.ServiceRegistrar;
import org.pminin.oanda.bot.services.impl.AccountServiceImpl;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@Configuration
@EnableScheduling
public class ConfigurationProcessor {

    @Bean
    public AccountService accountService(BotProperties botProperties) throws AccountException {
        return new AccountServiceImpl(botProperties.getAccount());
    }

    @Bean
    @DependsOn("accountService")
    public ServiceRegistrar serviceRegistrar(BotProperties botProperties, ApplicationContext context) {
        return new ServiceRegistrar(botProperties, context);
    }

}
