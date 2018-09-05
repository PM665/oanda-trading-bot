package org.pminin.oanda.bot.config;

import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.primitives.Direction;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Data
@Component
@ConfigurationProperties("org.pminin.bot")
@EnableConfigurationProperties
public class BotProperties {

    private AccountDefinition account;

    private List<StrategyDefinition> strategies;

    private List<TraderOptions> traderOptions;

    @Data
    public static class StrategyDefinition {

        private String name;
        private boolean enabled;
        private String description;
        private List<String> trigger;
        private Direction direction;
        private CandlestickGranularity granularity;
        private String takeProfit;
        private String stopLoss;
        private int maxTradesOpen;
        private int maPeriod;
        private int shift;
    }

    @Data
    public static class AccountDefinition {

        private String name;
        private String accountId;
        private String url;
        private String token;
    }

    @Data
    public static class TraderOptions {

        private String accountId;
        private Map<String, Set<String>> traders;
    }

}
