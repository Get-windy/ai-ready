package cn.aiedge.notification.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步通知配置
 * 
 * @author AI-Ready Team
 * @since 1.1.0
 */
@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncNotificationConfig implements AsyncConfigurer {

    private final NotificationProperties properties;

    /**
     * 通知发送专用线程池
     */
    @Bean(name = "notificationExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getAsyncThreadPoolSize());
        executor.setMaxPoolSize(properties.getAsyncThreadPoolSize() * 2);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("notification-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        log.info("通知线程池初始化完成: corePoolSize={}", properties.getAsyncThreadPoolSize());
        return executor;
    }

    /**
     * 异步异常处理
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("异步通知发送异常: method={}, params={}", method.getName(), params, throwable);
        };
    }
}
