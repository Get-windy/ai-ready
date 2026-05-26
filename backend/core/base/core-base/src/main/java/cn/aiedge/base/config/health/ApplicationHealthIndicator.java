package cn.aiedge.base.config.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;

/**
 * 应用健康检查指示器
 * 
 * 检查项：
 * 1. JVM 内存使用情况
 * 2. CPU 使用率
 * 3. 应用启动时间
 * 4. 活跃线程数
 * 
 * @author devops-engineer
 * @since 1.0.0
 */
@Component
public class ApplicationHealthIndicator implements HealthIndicator {

    private final Environment environment;

    public ApplicationHealthIndicator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Health health() {
        try {
            // JVM 内存信息
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            double heapRatio = (heapUsed * 100.0) / heapMax;

            // 系统信息
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getSystemLoadAverage();
            int availableProcessors = osBean.getAvailableProcessors();

            // 线程信息
            int threadCount = ManagementFactory.getThreadMXBean().getThreadCount();

            // 应用信息
            String appName = environment.getProperty("spring.application.name", "ai-ready");
            String activeProfile = environment.getProperty("spring.profiles.active", "default");

            Health.Builder builder = Health.up()
                    .withDetail("application", appName)
                    .withDetail("profile", activeProfile)
                    .withDetail("heapUsedMB", heapUsed / (1024 * 1024))
                    .withDetail("heapMaxMB", heapMax / (1024 * 1024))
                    .withDetail("heapUsagePercent", String.format("%.2f%%", heapRatio))
                    .withDetail("systemLoadAverage", cpuLoad)
                    .withDetail("availableProcessors", availableProcessors)
                    .withDetail("activeThreadCount", threadCount);

            // 内存警告阈值 (80%)
            if (heapRatio > 80) {
                builder.withDetail("warning", "JVM heap memory usage is high");
            }

            // CPU 负载警告
            if (cpuLoad > availableProcessors * 0.8) {
                builder.withDetail("warning", "System CPU load is high");
            }

            return builder.build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}