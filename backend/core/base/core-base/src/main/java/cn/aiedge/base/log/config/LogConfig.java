package cn.aiedge.base.log.config;

import cn.aiedge.base.log.aspect.OperationLogAspect;
import cn.aiedge.base.log.service.SystemLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 日志模块配置类
 * 配置日志相关的组件和服务
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
@EnableAsync
public class LogConfig {

    /**
     * 配置操作日志切面
     * 
     * @param systemLogService 系统日志服务
     * @param objectMapper JSON对象映射器
     * @return 操作日志切面实例
     */
    @Bean
    @ConditionalOnMissingBean(OperationLogAspect.class)
    public OperationLogAspect operationLogAspect(SystemLogService systemLogService, ObjectMapper objectMapper) {
        return new OperationLogAspect(systemLogService, objectMapper);
    }

    /**
     * 配置日志清理任务
     * 可以在这里添加定时清理日志的任务
     */
    // @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    // public void cleanExpiredLogs() {
    //     // 实现日志清理逻辑
    // }
}