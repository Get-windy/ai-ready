package cn.aiedge.base.config.metrics;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 数据库连接池监控指标配置
 * 
 * 指标类型：
 * 1. 活跃连接数 - db_pool_active_connections
 * 2. 空闲连接数 - db_pool_idle_connections
 * 3. 总连接数 - db_pool_total_connections
 * 4. 最大连接数 - db_pool_max_connections
 * 5. 等待连接数 - db_pool_pending_connections
 * 
 * @author devops-engineer
 * @since 1.0.0
 */
@Configuration
public class DatabaseMetricsConfig {

    public static final String DB_POOL_ACTIVE = "ai_ready_db_pool_active_connections";
    public static final String DB_POOL_IDLE = "ai_ready_db_pool_idle_connections";
    public static final String DB_POOL_TOTAL = "ai_ready_db_pool_total_connections";
    public static final String DB_POOL_MAX = "ai_ready_db_pool_max_connections";
    public static final String DB_POOL_PENDING = "ai_ready_db_pool_pending_connections";

    @Autowired(required = false)
    private DataSource dataSource;

    @Bean
    public Gauge dbPoolActiveGauge(MeterRegistry registry) {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            return Gauge.builder(DB_POOL_ACTIVE, () -> 
                    hikariDataSource.getHikariPoolMXBean().getActiveConnections())
                    .description("Number of active database connections")
                    .tags("pool", "hikari")
                    .register(registry);
        }
        return null;
    }

    @Bean
    public Gauge dbPoolIdleGauge(MeterRegistry registry) {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            return Gauge.builder(DB_POOL_IDLE, () -> 
                    hikariDataSource.getHikariPoolMXBean().getIdleConnections())
                    .description("Number of idle database connections")
                    .tags("pool", "hikari")
                    .register(registry);
        }
        return null;
    }

    @Bean
    public Gauge dbPoolTotalGauge(MeterRegistry registry) {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            return Gauge.builder(DB_POOL_TOTAL, () -> 
                    hikariDataSource.getHikariPoolMXBean().getTotalConnections())
                    .description("Total number of database connections")
                    .tags("pool", "hikari")
                    .register(registry);
        }
        return null;
    }

    @Bean
    public Gauge dbPoolMaxGauge(MeterRegistry registry) {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            return Gauge.builder(DB_POOL_MAX, () -> 
                    hikariDataSource.getMaximumPoolSize())
                    .description("Maximum number of database connections")
                    .tags("pool", "hikari")
                    .register(registry);
        }
        return null;
    }

    @Bean
    public Gauge dbPoolPendingGauge(MeterRegistry registry) {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            return Gauge.builder(DB_POOL_PENDING, () -> 
                    hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection())
                    .description("Number of threads awaiting database connections")
                    .tags("pool", "hikari")
                    .register(registry);
        }
        return null;
    }
}