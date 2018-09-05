package org.pminin.oanda.bot.config;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.pminin.oanda.bot.services.CollectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@DependsOn("serviceRegistrar")
public class SchedulerService {

    private List<CollectorService> collectorServices;

    @Autowired
    public SchedulerService(List<CollectorService> collectorServices) {
        this.collectorServices = collectorServices;
    }

    @Bean(destroyMethod = "shutdown")
    public Executor taskScheduler() {
        return Executors.newScheduledThreadPool(42);
    }

    @Scheduled(fixedDelayString = "${org.pminin.bot.collector.collectInterval}")
    public void collectTrigger() {
        collectorServices.forEach(CollectorService::collectCandles);
    }

    @Scheduled(fixedDelayString = "${org.pminin.bot.collector.cutInterval}")
    public void cutCandles() {
        collectorServices.forEach(CollectorService::cutCandles);
    }
}
