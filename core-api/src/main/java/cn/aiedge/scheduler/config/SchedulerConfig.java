package cn.aiedge.scheduler.config;

import org.quartz.Scheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * Quartz调度器配置
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
public class SchedulerConfig {

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setOverwriteExistingJobs(true);
        factory.setAutoStartup(true);
        factory.setStartupDelay(2);
        return factory;
    }

    @Bean
    public Scheduler scheduler(SchedulerFactoryBean factory) {
        return factory.getObject();
    }
}
