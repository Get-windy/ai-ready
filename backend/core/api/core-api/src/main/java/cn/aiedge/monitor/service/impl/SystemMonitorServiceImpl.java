package cn.aiedge.monitor.service.impl;

import cn.aiedge.monitor.model.SystemMetrics;
import cn.aiedge.monitor.service.SystemMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 系统监控服务实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class SystemMonitorServiceImpl implements SystemMonitorService {

    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

    // 存储历史指标（实际应使用时序数据库）
    private final LinkedList<SystemMetrics> metricsHistory = new LinkedList<>();
    private static final int MAX_HISTORY_SIZE = 1000;

    @Override
    public SystemMetrics getCurrentMetrics() {
        SystemMetrics metrics = new SystemMetrics();
        metrics.setCollectTime(LocalDateTime.now());

        // CPU指标
        collectCpuMetrics(metrics);

        // 内存指标
        collectMemoryMetrics(metrics);

        // 磁盘指标
        collectDiskMetrics(metrics);

        // JVM指标
        collectJvmMetrics(metrics);

        // 线程指标
        collectThreadMetrics(metrics);

        // GC指标
        collectGcMetrics(metrics);

        // 系统负载
        collectLoadMetrics(metrics);

        // 保存历史
        synchronized (metricsHistory) {
            metricsHistory.addLast(metrics);
            if (metricsHistory.size() > MAX_HISTORY_SIZE) {
                metricsHistory.removeFirst();
            }
        }

        return metrics;
    }

    @Override
    public List<SystemMetrics> getHistoryMetrics(int hours) {
        synchronized (metricsHistory) {
            return new ArrayList<>(metricsHistory);
        }
    }

    @Override
    public Map<String, Object> getMetricTrend(String metricName, int hours) {
        Map<String, Object> result = new HashMap<>();
        List<Object> values = new ArrayList<>();
        List<String> times = new ArrayList<>();

        synchronized (metricsHistory) {
            for (SystemMetrics m : metricsHistory) {
                times.add(m.getCollectTime().toString());
                values.add(getMetricValue(m, metricName));
            }
        }

        result.put("metricName", metricName);
        result.put("times", times);
        result.put("values", values);
        return result;
    }

    @Override
    public Map<String, Object> getSystemOverview() {
        Map<String, Object> overview = new HashMap<>();

        SystemMetrics metrics = getCurrentMetrics();

        overview.put("cpuUsage", metrics.getCpuUsage());
        overview.put("memoryUsage", metrics.getMemoryUsage());
        overview.put("diskUsage", metrics.getDiskUsage());
        overview.put("threadCount", metrics.getThreadCount());
        overview.put("systemLoad", metrics.getSystemLoad1());
        overview.put("jvmUptime", formatUptime(metrics.getJvmUptime()));
        overview.put("collectTime", metrics.getCollectTime());

        // 系统状态判断
        String status = "healthy";
        if (metrics.getCpuUsage() != null && metrics.getCpuUsage() > 80) status = "warning";
        if (metrics.getMemoryUsage() != null && metrics.getMemoryUsage() > 85) status = "warning";
        if (metrics.getCpuUsage() != null && metrics.getCpuUsage() > 95) status = "critical";
        if (metrics.getMemoryUsage() != null && metrics.getMemoryUsage() > 95) status = "critical";
        overview.put("status", status);

        return overview;
    }

    @Override
    public Map<String, Object> checkHealth() {
        Map<String, Object> health = new HashMap<>();

        SystemMetrics metrics = getCurrentMetrics();

        // CPU健康检查
        Map<String, Object> cpuHealth = new HashMap<>();
        cpuHealth.put("status", metrics.getCpuUsage() < 80 ? "UP" : "WARNING");
        cpuHealth.put("usage", metrics.getCpuUsage());
        health.put("cpu", cpuHealth);

        // 内存健康检查
        Map<String, Object> memoryHealth = new HashMap<>();
        memoryHealth.put("status", metrics.getMemoryUsage() < 85 ? "UP" : "WARNING");
        memoryHealth.put("usage", metrics.getMemoryUsage());
        health.put("memory", memoryHealth);

        // 磁盘健康检查
        Map<String, Object> diskHealth = new HashMap<>();
        diskHealth.put("status", metrics.getDiskUsage() < 90 ? "UP" : "WARNING");
        diskHealth.put("usage", metrics.getDiskUsage());
        health.put("disk", diskHealth);

        // 磁盘空间检查
        Map<String, Object> spaceHealth = new HashMap<>();
        spaceHealth.put("status", metrics.getFreeDisk() > 1 ? "UP" : "DOWN");
        spaceHealth.put("freeSpace", metrics.getFreeDisk());
        health.put("diskSpace", spaceHealth);

        // 总体状态
        boolean allUp = health.values().stream()
                .filter(v -> v instanceof Map)
                .map(v -> (Map<String, Object>) v)
                .allMatch(m -> "UP".equals(m.get("status")));
        health.put("status", allUp ? "UP" : "WARNING");

        return health;
    }

    @Override
    public Map<String, Object> getJvmInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("javaVersion", System.getProperty("java.version"));
        info.put("javaVendor", System.getProperty("java.vendor"));
        info.put("javaHome", System.getProperty("java.home"));
        info.put("javaVmName", System.getProperty("java.vm.name"));
        info.put("javaVmVersion", System.getProperty("java.vm.version"));

        // JVM参数
        info.put("vmArguments", runtimeBean.getInputArguments());

        // JVM启动时间
        info.put("startTime", LocalDateTime.now().minusNanos(runtimeBean.getUptime() * 1_000_000));

        return info;
    }

    @Override
    public Map<String, Object> getThreadInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("threadCount", threadBean.getThreadCount());
        info.put("peakThreadCount", threadBean.getPeakThreadCount());
        info.put("daemonThreadCount", threadBean.getDaemonThreadCount());
        info.put("totalStartedThreadCount", threadBean.getTotalStartedThreadCount());

        // 死锁检测
        long[] deadlockedThreads = threadBean.findDeadlockedThreads();
        info.put("deadlockedThreads", deadlockedThreads != null ? deadlockedThreads.length : 0);

        return info;
    }

    @Override
    public Map<String, Object> getMemoryInfo() {
        Map<String, Object> info = new HashMap<>();

        // 堆内存
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        Map<String, Long> heap = new HashMap<>();
        heap.put("init", heapUsage.getInit() / 1024 / 1024);
        heap.put("used", heapUsage.getUsed() / 1024 / 1024);
        heap.put("committed", heapUsage.getCommitted() / 1024 / 1024);
        heap.put("max", heapUsage.getMax() / 1024 / 1024);
        info.put("heap", heap);

        // 非堆内存
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        Map<String, Long> nonHeap = new HashMap<>();
        nonHeap.put("init", nonHeapUsage.getInit() / 1024 / 1024);
        nonHeap.put("used", nonHeapUsage.getUsed() / 1024 / 1024);
        nonHeap.put("committed", nonHeapUsage.getCommitted() / 1024 / 1024);
        nonHeap.put("max", nonHeapUsage.getMax() / 1024 / 1024);
        info.put("nonHeap", nonHeap);

        return info;
    }

    @Override
    public void performGc() {
        log.info("Manual GC triggered");
        System.gc();
    }

    // ==================== 采集方法 ====================

    private void collectCpuMetrics(SystemMetrics metrics) {
        metrics.setCpuCores(osBean.getAvailableProcessors());
        
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            metrics.setCpuUsage(sunOsBean.getCpuLoad() * 100);
            metrics.setSystemCpuUsage(sunOsBean.getSystemCpuLoad() * 100);
            metrics.setProcessCpuUsage(sunOsBean.getProcessCpuLoad() * 100);
        }
    }

    private void collectMemoryMetrics(SystemMetrics metrics) {
        Runtime runtime = Runtime.getRuntime();
        
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;

        metrics.setTotalMemory(totalMemory);
        metrics.setFreeMemory(freeMemory);
        metrics.setUsedMemory(usedMemory);
        metrics.setMemoryUsage((double) usedMemory / totalMemory * 100);
        metrics.setHeapUsed(usedMemory);
        metrics.setHeapMax(maxMemory);

        // 系统内存
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            long totalPhys = sunOsBean.getTotalPhysicalMemorySize() / 1024 / 1024;
            long freePhys = sunOsBean.getFreePhysicalMemorySize() / 1024 / 1024;
            metrics.setTotalMemory(totalPhys);
            metrics.setFreeMemory(freePhys);
            metrics.setMemoryUsage((double) (totalPhys - freePhys) / totalPhys * 100);
        }
    }

    private void collectDiskMetrics(SystemMetrics metrics) {
        java.io.File root = new java.io.File("/");
        long totalSpace = root.getTotalSpace() / 1024 / 1024 / 1024;
        long freeSpace = root.getFreeSpace() / 1024 / 1024 / 1024;
        long usableSpace = root.getUsableSpace() / 1024 / 1024 / 1024;
        long usedSpace = totalSpace - freeSpace;

        metrics.setTotalDisk(totalSpace);
        metrics.setFreeDisk(usableSpace);
        metrics.setUsedDisk(usedSpace);
        metrics.setDiskUsage((double) usedSpace / totalSpace * 100);
    }

    private void collectJvmMetrics(SystemMetrics metrics) {
        metrics.setJvmUptime(runtimeBean.getUptime());
        metrics.setJvmStartTime(LocalDateTime.now().minusNanos(runtimeBean.getUptime() * 1_000_000));
    }

    private void collectThreadMetrics(SystemMetrics metrics) {
        metrics.setThreadCount(threadBean.getThreadCount());
        metrics.setPeakThreadCount(threadBean.getPeakThreadCount());
        metrics.setDaemonThreadCount(threadBean.getDaemonThreadCount());
    }

    private void collectGcMetrics(SystemMetrics metrics) {
        long totalGcCount = 0;
        long totalGcTime = 0;

        for (GarbageCollectorMXBean gcBean : gcBeans) {
            totalGcCount += gcBean.getCollectionCount();
            totalGcTime += gcBean.getCollectionTime();
        }

        metrics.setGcCount(totalGcCount);
        metrics.setGcTime(totalGcTime);
    }

    private void collectLoadMetrics(SystemMetrics metrics) {
        double loadAverage = osBean.getSystemLoadAverage();
        if (loadAverage >= 0) {
            metrics.setSystemLoad1(loadAverage);
            metrics.setSystemLoad5(loadAverage); // 简化，实际应分别获取
            metrics.setSystemLoad15(loadAverage);
        }
    }

    private Object getMetricValue(SystemMetrics metrics, String metricName) {
        return switch (metricName) {
            case "cpuUsage" -> metrics.getCpuUsage();
            case "memoryUsage" -> metrics.getMemoryUsage();
            case "diskUsage" -> metrics.getDiskUsage();
            case "threadCount" -> metrics.getThreadCount();
            case "systemLoad" -> metrics.getSystemLoad1();
            case "gcCount" -> metrics.getGcCount();
            default -> null;
        };
    }

    private String formatUptime(Long uptimeMs) {
        if (uptimeMs == null) return "N/A";
        long seconds = uptimeMs / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        return String.format("%dd %dh %dm", days, hours, minutes);
    }
}
