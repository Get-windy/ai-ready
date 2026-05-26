package cn.aiedge.base.config;

import cn.aiedge.base.log.config.LogConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 日志审计模块自动配置
 * 自动启用日志审计功能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
@Import(LogConfig.class)
@ComponentScan(basePackages = "cn.aiedge.base.log")
public class LogAuditAutoConfig {
    // 自动配置类，用于启用日志审计功能
}