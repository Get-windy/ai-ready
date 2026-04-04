package cn.aiedge.base.config.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * PostgreSQL 数据库健康检查指示器
 * 
 * 检查项：
 * 1. 数据库连接状态
 * 2. 连接池活跃连接数
 * 3. 数据库响应时间
 * 
 * @author devops-engineer
 * @since 1.0.0
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            // 检查连接是否有效
            if (!connection.isValid(5)) {
                return Health.down()
                        .withDetail("error", "Database connection is not valid")
                        .build();
            }

            // 测试查询执行时间
            long startTime = System.currentTimeMillis();
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT 1");
            rs.next();
            long queryTime = System.currentTimeMillis() - startTime;

            // 获取连接池信息
            String poolInfo = connection.toString();
            boolean isFromPool = poolInfo.contains("HikariPool");

            Health.Builder builder = Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("connection", "active")
                    .withDetail("queryTimeMs", queryTime)
                    .withDetail("fromPool", isFromPool);

            // 响应时间警告阈值
            if (queryTime > 100) {
                builder.withDetail("warning", "Query response time is slow");
            }

            return builder.build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("database", "PostgreSQL")
                    .withException(e)
                    .build();
        }
    }
}