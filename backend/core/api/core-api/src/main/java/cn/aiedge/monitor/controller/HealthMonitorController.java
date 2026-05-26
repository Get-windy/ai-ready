package cn.aiedge.monitor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 服务健康状态监控控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/monitor/health")
@RequiredArgsConstructor
@Tag(name = "服务健康监控", description = "服务健康状态、依赖服务状态监控接口")
public class HealthMonitorController {

    private final HealthEndpoint healthEndpoint;

    @Autowired(required = false)
    private DataSource dataSource;

    /**
     * 获取综合健康状态
     */
    @GetMapping("/status")
    @Operation(summary = "获取服务综合健康状态")
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> result = new HashMap<>();
        
        HealthComponent health = healthEndpoint.health();
        
        result.put("status", health.getStatus().getCode());
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("serviceName", "ai-ready-core-api");
        
        // 详细健康信息
        if (health instanceof Health healthDetails) {
            Map<String, Object> componentStatus = new HashMap<>();
            // 简化实现，不获取组件详情，只返回基本状态
            result.put("components", componentStatus);
        }
        
        // 计算健康分数
        int score = calculateHealthScore(result);
        result.put("healthScore", score);
        result.put("healthLevel", getHealthLevel(score));
        
        return result;
    }

    /**
     * 获取数据库健康状态
     */
    @GetMapping("/database")
    @Operation(summary = "获取数据库健康状态")
    public Map<String, Object> getDatabaseHealth() {
        Map<String, Object> result = new HashMap<>();
        
        result.put("timestamp", LocalDateTime.now().toString());
        
        if (dataSource == null) {
            result.put("status", "UNKNOWN");
            result.put("message", "DataSource not available");
            return result;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 测试连接
            try (var connection = dataSource.getConnection()) {
                boolean valid = connection.isValid(5);
                long responseTime = System.currentTimeMillis() - startTime;
                
                result.put("status", valid ? "UP" : "DOWN");
                result.put("responseTimeMs", responseTime);
                result.put("databaseProductName", connection.getMetaData().getDatabaseProductName());
                result.put("databaseProductVersion", connection.getMetaData().getDatabaseProductVersion());
                
                // 连接池信息（如果可用）
                try {
                    result.put("autoCommit", connection.getAutoCommit());
                    result.put("transactionIsolation", connection.getTransactionIsolation());
                } catch (Exception e) {
                    // 忽略
                }
            }
        } catch (Exception e) {
            log.error("Database health check failed", e);
            result.put("status", "DOWN");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取JVM健康状态
     */
    @GetMapping("/jvm")
    @Operation(summary = "获取JVM健康状态")
    public Map<String, Object> getJvmHealth() {
        Map<String, Object> result = new HashMap<>();
        
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        
        // 堆内存状态
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        Map<String, Object> heap = new HashMap<>();
        heap.put("init", heapUsage.getInit());
        heap.put("used", heapUsage.getUsed());
        heap.put("committed", heapUsage.getCommitted());
        heap.put("max", heapUsage.getMax());
        heap.put("usagePercent", calculateUsagePercent(heapUsage.getUsed(), heapUsage.getMax()));
        
        // 非堆内存状态
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        Map<String, Object> nonHeap = new HashMap<>();
        nonHeap.put("init", nonHeapUsage.getInit());
        nonHeap.put("used", nonHeapUsage.getUsed());
        nonHeap.put("committed", nonHeapUsage.getCommitted());
        nonHeap.put("max", nonHeapUsage.getMax());
        
        // 线程状态
        Map<String, Object> threads = new HashMap<>();
        threads.put("threadCount", threadBean.getThreadCount());
        threads.put("peakThreadCount", threadBean.getPeakThreadCount());
        threads.put("daemonThreadCount", threadBean.getDaemonThreadCount());
        threads.put("totalStartedThreadCount", threadBean.getTotalStartedThreadCount());
        
        // 死锁检测
        long[] deadlockedIds = threadBean.findDeadlockedThreads();
        threads.put("deadlockedThreadCount", deadlockedIds != null ? deadlockedIds.length : 0);
        
        // 状态评估
        String status = "UP";
        List<String> warnings = new ArrayList<>();
        
        double heapUsagePercent = calculateUsagePercent(heapUsage.getUsed(), heapUsage.getMax());
        if (heapUsagePercent > 90) {
            status = "CRITICAL";
            warnings.add("Heap memory usage is critical: " + String.format("%.2f%%", heapUsagePercent));
        } else if (heapUsagePercent > 80) {
            status = "WARNING";
            warnings.add("Heap memory usage is high: " + String.format("%.2f%%", heapUsagePercent));
        }
        
        if (deadlockedIds != null && deadlockedIds.length > 0) {
            status = "CRITICAL";
            warnings.add("Deadlocked threads detected: " + deadlockedIds.length);
        }
        
        result.put("status", status);
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("heapMemory", heap);
        result.put("nonHeapMemory", nonHeap);
        result.put("threads", threads);
        result.put("warnings", warnings);
        
        return result;
    }

    /**
     * 获取磁盘健康状态
     */
    @GetMapping("/disk")
    @Operation(summary = "获取磁盘健康状态")
    public Map<String, Object> getDiskHealth() {
        Map<String, Object> result = new HashMap<>();
        
        File[] roots = File.listRoots();
        List<Map<String, Object>> diskInfos = new ArrayList<>();
        
        String overallStatus = "UP";
        
        for (File root : roots) {
            Map<String, Object> disk = new HashMap<>();
            
            long total = root.getTotalSpace();
            long free = root.getFreeSpace();
            long used = total - free;
            double usagePercent = total > 0 ? (double) used / total * 100 : 0;
            
            disk.put("path", root.getAbsolutePath());
            disk.put("totalSpace", total);
            disk.put("freeSpace", free);
            disk.put("usedSpace", used);
            disk.put("usagePercent", usagePercent);
            
            // 状态判断
            String diskStatus = "UP";
            if (usagePercent > 95) {
                diskStatus = "CRITICAL";
                overallStatus = "CRITICAL";
            } else if (usagePercent > 90) {
                diskStatus = "WARNING";
                if (!"CRITICAL".equals(overallStatus)) {
                    overallStatus = "WARNING";
                }
            }
            
            disk.put("status", diskStatus);
            diskInfos.add(disk);
        }
        
        result.put("status", overallStatus);
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("disks", diskInfos);
        
        return result;
    }

    /**
     * 获取依赖服务健康状态
     */
    @GetMapping("/dependencies")
    @Operation(summary = "获取依赖服务健康状态")
    public Map<String, Object> getDependenciesHealth() {
        Map<String, Object> result = new HashMap<>();
        
        // 这里应该检查实际的依赖服务
        // 简化实现，返回框架
        Map<String, Object> dependencies = new HashMap<>();
        
        // 数据库
        dependencies.put("database", checkDependency("Database", dataSource != null));
        
        // Redis（如果有）
        dependencies.put("redis", checkDependency("Redis", false)); // 需要实际检查
        
        // 消息队列（如果有）
        dependencies.put("messageQueue", checkDependency("Message Queue", false)); // 需要实际检查
        
        // 外部API（如果有）
        dependencies.put("externalApi", checkDependency("External API", true)); // 假设可用
        
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("dependencies", dependencies);
        
        // 计算总体状态
        boolean allUp = dependencies.values().stream()
                .map(d -> (Map<String, Object>) d)
                .allMatch(d -> "UP".equals(d.get("status")));
        
        result.put("status", allUp ? "UP" : "DEGRADED");
        
        return result;
    }

    /**
     * 获取就绪状态（Readiness Probe）
     */
    @GetMapping("/ready")
    @Operation(summary = "获取服务就绪状态（Kubernetes Readiness Probe）")
    public Map<String, Object> getReadiness() {
        Map<String, Object> result = new HashMap<>();
        
        boolean ready = true;
        List<String> checks = new ArrayList<>();
        
        // 检查数据库连接
        if (dataSource != null) {
            try (var conn = dataSource.getConnection()) {
                if (conn.isValid(5)) {
                    checks.add("Database: OK");
                } else {
                    checks.add("Database: FAILED");
                    ready = false;
                }
            } catch (Exception e) {
                checks.add("Database: ERROR - " + e.getMessage());
                ready = false;
            }
        }
        
        result.put("ready", ready);
        result.put("status", ready ? "UP" : "DOWN");
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("checks", checks);
        
        return result;
    }

    /**
     * 获取存活状态（Liveness Probe）
     */
    @GetMapping("/live")
    @Operation(summary = "获取服务存活状态（Kubernetes Liveness Probe）")
    public Map<String, Object> getLiveness() {
        Map<String, Object> result = new HashMap<>();
        
        // 基本存活检查
        boolean alive = true;
        
        // 检查JVM内存是否耗尽
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        if (heapUsage.getUsed() > heapUsage.getMax() * 0.99) {
            alive = false;
        }
        
        // 检查死锁
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long[] deadlocked = threadBean.findDeadlockedThreads();
        if (deadlocked != null && deadlocked.length > 10) {
            alive = false;
        }
        
        result.put("alive", alive);
        result.put("status", alive ? "UP" : "DOWN");
        result.put("timestamp", LocalDateTime.now().toString());
        
        return result;
    }

    // ==================== 私有方法 ====================

    private int calculateHealthScore(Map<String, Object> healthResult) {
        String status = (String) healthResult.get("status");
        
        if ("UP".equals(status)) {
            return 100;
        } else if ("DEGRADED".equals(status)) {
            return 70;
        } else if ("DOWN".equals(status)) {
            return 0;
        }
        
        return 50;
    }

    private String getHealthLevel(int score) {
        if (score >= 90) return "EXCELLENT";
        if (score >= 70) return "GOOD";
        if (score >= 50) return "FAIR";
        if (score >= 30) return "POOR";
        return "CRITICAL";
    }

    private double calculateUsagePercent(long used, long max) {
        if (max <= 0) return 0;
        return (double) used / max * 100;
    }

    private Map<String, Object> checkDependency(String name, boolean available) {
        Map<String, Object> dep = new HashMap<>();
        dep.put("name", name);
        dep.put("status", available ? "UP" : "DOWN");
        dep.put("timestamp", LocalDateTime.now().toString());
        return dep;
    }
}