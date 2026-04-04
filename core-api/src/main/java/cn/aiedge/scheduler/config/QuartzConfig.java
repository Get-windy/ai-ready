package cn.aiedge.scheduler.config;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz调度器配置
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
public class QuartzConfig {

    @Bean
    public Scheduler scheduler() throws SchedulerException {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.getListenerManager().addJobListener(new TaskExecutionListener());
        return scheduler;
    }
}
