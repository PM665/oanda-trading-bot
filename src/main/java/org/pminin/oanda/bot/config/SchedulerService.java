package org.pminin.oanda.bot.config;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.pminin.oanda.bot.services.CollectorService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Scheduled(fixedDelay = 2000)
    public void collectTrigger() {
        collectorServices.forEach(CollectorService::collectCandles);
    }

}
