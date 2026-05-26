package cn.aiedge.scheduler.config;

import cn.aiedge.scheduler.config.TaskExecutionListener;
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
    public Scheduler scheduler(TaskExecutionListener taskExecutionListener) throws SchedulerException {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.getListenerManager().addJobListener(taskExecutionListener);
        return scheduler;
    }
}
