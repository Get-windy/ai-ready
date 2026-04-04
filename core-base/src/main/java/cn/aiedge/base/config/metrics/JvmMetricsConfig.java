package cn.aiedge.base.config.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;

/**
 * JVM监控指标配置
 * 
 * 指标类型：
 * 1. JVM堆内存使用 - jvm_heap_used_bytes
 * 2. JVM堆内存最大 - jvm_heap_max_bytes
 * 3. JVM非堆内存 - jvm_nonheap_used_bytes
 * 4. 活跃线程数 - jvm_threads_active
 * 5. 峰值线程数 - jvm_threads_peak
 * 
 * @author devops-engineer
 * @since 1.0.0
 */
@Configuration
public class JvmMetricsConfig {

    public static final String JVM_HEAP_USED = "jvm_heap_used_bytes";
    public static final String JVM_HEAP_MAX = "jvm_heap_max_bytes";
    public static final String JVM_HEAP_USAGE_RATIO = "jvm_heap_usage_ratio";
    public static final String JVM_NONHEAP_USED = "jvm_nonheap_used_bytes";
    public static final String JVM_THREADS_ACTIVE = "jvm_threads_active";
    public static final String JVM_THREADS_PEAK = "jvm_threads_peak";
    public static final String JVM_THREADS_DAEMON = "jvm_threads_daemon";

    @Bean
    public Gauge jvmHeapUsedGauge(MeterRegistry registry) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        return Gauge.builder(JVM_HEAP_USED, () -> memoryBean.getHeapMemoryUsage().getUsed())
                .description("JVM heap memory used in bytes")
                .tags("area", "heap")
                .register(registry);
    }

    @Bean
    public Gauge jvmHeapMaxGauge(MeterRegistry registry) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        return Gauge.builder(JVM_HEAP_MAX, () -> memoryBean.getHeapMemoryUsage().getMax())
                .description("JVM heap memory max in bytes")
                .tags("area", "heap")
                .register(registry);
    }

    @Bean
    public Gauge jvmHeapUsageRatioGauge(MeterRegistry registry) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        return Gauge.builder(JVM_HEAP_USAGE_RATIO, () -> {
            MemoryUsage usage = memoryBean.getHeapMemoryUsage();
            if (usage.getMax() <= 0) return 0.0;
            return (usage.getUsed() * 100.0) / usage.getMax();
        })
                .description("JVM heap memory usage percentage")
                .tags("area", "heap")
                .register(registry);
    }

    @Bean
    public Gauge jvmNonHeapUsedGauge(MeterRegistry registry) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        return Gauge.builder(JVM_NONHEAP_USED, () -> memoryBean.getNonHeapMemoryUsage().getUsed())
                .description("JVM non-heap memory used in bytes")
                .tags("area", "nonheap")
                .register(registry);
    }

    @Bean
    public Gauge jvmThreadsActiveGauge(MeterRegistry registry) {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        return Gauge.builder(JVM_THREADS_ACTIVE, threadBean::getThreadCount)
                .description("Current number of live threads")
                .register(registry);
    }

    @Bean
    public Gauge jvmThreadsPeakGauge(MeterRegistry registry) {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        return Gauge.builder(JVM_THREADS_PEAK, threadBean::getPeakThreadCount)
                .description("Peak live thread count")
                .register(registry);
    }

    @Bean
    public Gauge jvmThreadsDaemonGauge(MeterRegistry registry) {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        return Gauge.builder(JVM_THREADS_DAEMON, threadBean::getDaemonThreadCount)
                .description("Current number of daemon threads")
                .register(registry);
    }
}