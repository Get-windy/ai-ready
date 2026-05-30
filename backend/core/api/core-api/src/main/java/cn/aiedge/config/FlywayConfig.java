package cn.aiedge.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway 数据库版本管理配置
 * 确保每次启动时自动执行未完成的迁移脚本
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // 执行迁移，如果失败则抛出异常阻止应用启动
            flyway.migrate();
        };
    }
}
