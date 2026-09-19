package cn.aiedge.datasource.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 数据管理异步执行线程池
 *
 * <p>备份管理页的「创建备份」不能在 HTTP 请求线程里同步等 {@code pg_dump} 跑完
 * （整库 dump 可能几分钟到几十分钟）—— 接口立即返回「已受理」，由本线程池在后台执行，
 * 页面靠轮询/回读台账看状态。
 *
 * <p>为什么要独立线程池（而不是用默认的 {@code @Async} 执行器）：
 * 备份是**长耗时 + 重 IO** 任务，落到 Spring 默认的 SimpleAsyncTaskExecutor 会无限建线程，
 * 与请求线程争抢资源；独立命名 + 有界队列 + 明确拒绝策略，故障时可在日志中一眼定位归属。
 *
 * <p>⚠️ 该线程池内**没有 Sa-Token 会话**，多租户拦截器会整体跳过 tenant_id 注入
 * （见 {@code AiReadyTenantLineInnerInterceptor#shouldSkip}）—— 因此异步任务里所有写操作
 * 都必须**显式带上 tenant_id 条件**，不能依赖拦截器兜底。
 */
@Slf4j
@Configuration
@EnableAsync
public class DataMaintenanceExecutorConfig {

    /** 线程池 Bean 名，供 {@code @Async("dataMaintenanceExecutor")} 引用 */
    public static final String EXECUTOR_NAME = "dataMaintenanceExecutor";

    @Bean(EXECUTOR_NAME)
    public Executor dataMaintenanceExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 备份并发度刻意压低：pg_dump 是磁盘/IO 密集型，并发过高会拖垮同实例的业务查询
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("data-maint-");
        // 队列满时由调用线程执行：宁可让 HTTP 请求慢一点，也不静默丢任务（丢任务＝台账永停 pending）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("数据管理异步线程池已初始化: name={}, core=2, max=4, queue=50", EXECUTOR_NAME);
        return executor;
    }
}
