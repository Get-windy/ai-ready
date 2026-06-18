package cn.aiedge.test.connectionpool.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * 数据源配置类
 * 配置多个不同并发级别的数据源用于性能测试
 */
@Slf4j
@Configuration
@Profile("test")
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.primary.hikari")
    public HikariConfig primaryHikariConfig() {
        return new HikariConfig();
    }

    @Bean
    @Primary
    public DataSource primaryDataSource(
            @Qualifier("primaryHikariConfig") HikariConfig config,
            ObjectProvider<MeterRegistry> meterRegistryProvider) {
        HikariDataSource dataSource = new HikariDataSource(config);
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry != null) {
            dataSource.setMetricRegistry(meterRegistry);
        }
        log.info("Primary datasource created with pool name: {}", config.getPoolName());
        return dataSource;
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.low-concurrency.hikari")
    public HikariConfig lowConcurrencyHikariConfig() {
        return new HikariConfig();
    }

    @Bean
    @Qualifier("lowConcurrencyDataSource")
    public DataSource lowConcurrencyDataSource(
            @Qualifier("lowConcurrencyHikariConfig") HikariConfig config,
            ObjectProvider<MeterRegistry> meterRegistryProvider) {
        HikariDataSource dataSource = new HikariDataSource(config);
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry != null) {
            dataSource.setMetricRegistry(meterRegistry);
        }
        log.info("Low concurrency datasource created with pool name: {}", config.getPoolName());
        return dataSource;
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.medium-concurrency.hikari")
    public HikariConfig mediumConcurrencyHikariConfig() {
        return new HikariConfig();
    }

    @Bean
    @Qualifier("mediumConcurrencyDataSource")
    public DataSource mediumConcurrencyDataSource(
            @Qualifier("mediumConcurrencyHikariConfig") HikariConfig config,
            ObjectProvider<MeterRegistry> meterRegistryProvider) {
        HikariDataSource dataSource = new HikariDataSource(config);
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry != null) {
            dataSource.setMetricRegistry(meterRegistry);
        }
        log.info("Medium concurrency datasource created with pool name: {}", config.getPoolName());
        return dataSource;
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.high-concurrency.hikari")
    public HikariConfig highConcurrencyHikariConfig() {
        return new HikariConfig();
    }

    @Bean
    @Qualifier("highConcurrencyDataSource")
    public DataSource highConcurrencyDataSource(
            @Qualifier("highConcurrencyHikariConfig") HikariConfig config,
            ObjectProvider<MeterRegistry> meterRegistryProvider) {
        HikariDataSource dataSource = new HikariDataSource(config);
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry != null) {
            dataSource.setMetricRegistry(meterRegistry);
        }
        log.info("High concurrency datasource created with pool name: {}", config.getPoolName());
        return dataSource;
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.h2.hikari")
    public HikariConfig h2HikariConfig() {
        return new HikariConfig();
    }

    @Bean
    @Qualifier("h2DataSource")
    public DataSource h2DataSource(
            @Qualifier("h2HikariConfig") HikariConfig config,
            ObjectProvider<MeterRegistry> meterRegistryProvider) {
        HikariDataSource dataSource = new HikariDataSource(config);
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry != null) {
            dataSource.setMetricRegistry(meterRegistry);
        }
        log.info("H2 datasource created with pool name: {}", config.getPoolName());
        return dataSource;
    }
}
