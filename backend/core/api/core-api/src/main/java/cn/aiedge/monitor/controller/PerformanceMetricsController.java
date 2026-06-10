package cn.aiedge.monitor.controller;

import io.swagger.v3.oas.annotations.Operation;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.lang.management.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 性能指标聚合分析控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/monitor/performance")
@RequiredArgsConstructor
@Tag(name = "性能指标分析", description = "性能指标聚合、分析和趋势预测接口")
@SaCheckLogin
public class PerformanceMetricsController {

    private final List<Map<String, Object>> metricsHistory = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_HISTORY_SIZE = 10000;

    /**
     * 获取实时性能指标
     */
    @GetMapping("/realtime")
    @Operation(summary = "获取实时性能指标")
    public Map<String, Object> getRealtimeMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // CPU指标
        metrics.put("cpu", collectCpuMetrics());
        
        // 内存指标
        metrics.put("memory", collectMemoryMetrics());
        
        // GC指标
        metrics.put("gc", collectGcMetrics());
        
        // 线程指标
        metrics.put("threads", collectThreadMetrics());
        
        // 类加载指标
        metrics.put("classLoading", collectClassLoadingMetrics());
        
        // 编译指标
        metrics.put("compilation", collectCompilationMetrics());
        
        metrics.put("timestamp", LocalDateTime.now().toString());
        
        // 保存历史
        saveMetricsHistory(metrics);
        
        return metrics;
    }

    /**
     * 获取性能聚合统计
     */
    @GetMapping("/aggregate")
    @Operation(summary = "获取性能指标聚合统计")
    public Map<String, Object> getAggregateMetrics(
            @Parameter(description = "时间窗口（分钟）")
            @RequestParam(defaultValue = "60") int windowMinutes) {
        Map<String, Object> result = new HashMap<>();
        
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(windowMinutes);
        
        // 获取历史数据
        List<Map<String, Object>> history = metricsHistory.stream()
                .filter(m -> {
                    String timestamp = (String) m.get("timestamp");
                    if (timestamp == null) return false;
                    LocalDateTime time = LocalDateTime.parse(timestamp);
                    return time.isAfter(cutoff);
                })
                .collect(Collectors.toList());
        
        if (history.isEmpty()) {
            result.put("message", "No data available for the specified time window");
            result.put("timestamp", LocalDateTime.now().toString());
            return result;
        }
        
        // CPU聚合
        result.put("cpu", aggregateCpuMetrics(history));
        
        // 内存聚合
        result.put("memory", aggregateMemoryMetrics(history));
        
        // GC聚合
        result.put("gc", aggregateGcMetrics(history));
        
        // 统计信息
        result.put("sampleCount", history.size());
        result.put("timeWindowMinutes", windowMinutes);
        result.put("timestamp", LocalDateTime.now().toString());
        
        return result;
    }

    /**
     * 获取性能趋势
     */
    @GetMapping("/trend/{metricType}")
    @Operation(summary = "获取性能趋势")
    public Map<String, Object> getPerformanceTrend(
            @PathVariable String metricType,
            @RequestParam(defaultValue = "60") int minutes) {
        Map<String, Object> result = new HashMap<>();
        
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutes);
        
        List<Map<String, Object>> history = metricsHistory.stream()
                .filter(m -> {
                    String timestamp = (String) m.get("timestamp");
                    if (timestamp == null) return false;
                    LocalDateTime time = LocalDateTime.parse(timestamp);
                    return time.isAfter(cutoff);
                })
                .collect(Collectors.toList());
        
        List<String> timestamps = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        
        for (Map<String, Object> m : history) {
            String ts = (String) m.get("timestamp");
            timestamps.add(ts);
            
            Double value = extractMetricValue(m, metricType);
            values.add(value != null ? value : 0.0);
        }
        
        result.put("metricType", metricType);
        result.put("timestamps", timestamps);
        result.put("values", values);
        result.put("sampleCount", values.size());
        
        // 计算趋势
        if (values.size() >= 2) {
            double trend = calculateTrend(values);
            result.put("trend", trend > 0 ? "increasing" : (trend < 0 ? "decreasing" : "stable"));
            result.put("trendSlope", trend);
        }
        
        // 统计值
        if (!values.isEmpty()) {
            result.put("min", values.stream().min(Double::compare).orElse(0.0));
            result.put("max", values.stream().max(Double::compare).orElse(0.0));
            result.put("avg", values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        }
        
        result.put("timestamp", LocalDateTime.now().toString());
        
        return result;
    }

    /**
     * 获取性能预测
     */
    @GetMapping("/predict/{metricType}")
    @Operation(summary = "获取性能预测")
    public Map<String, Object> predictPerformance(
            @PathVariable String metricType,
            @RequestParam(defaultValue = "30") int predictMinutes) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取最近的历史数据
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(60);
        
        List<Double> values = metricsHistory.stream()
                .filter(m -> {
                    String timestamp = (String) m.get("timestamp");
                    if (timestamp == null) return false;
                    LocalDateTime time = LocalDateTime.parse(timestamp);
                    return time.isAfter(cutoff);
                })
                .map(m -> extractMetricValue(m, metricType))
                .filter(v -> v != null)
                .collect(Collectors.toList());
        
        if (values.size() < 10) {
            result.put("message", "Insufficient data for prediction");
            result.put("timestamp", LocalDateTime.now().toString());
            return result;
        }
        
        // 简单线性回归预测
        double[] prediction = linearRegressionPredict(values, predictMinutes);
        
        result.put("metricType", metricType);
        result.put("currentValue", values.get(values.size() - 1));
        result.put("predictedValue", prediction[prediction.length - 1]);
        result.put("predictionHorizonMinutes", predictMinutes);
        result.put("confidence", calculateConfidence(values));
        result.put("timestamp", LocalDateTime.now().toString());
        
        // 告警预测
        if (metricType.contains("cpu") && prediction[prediction.length - 1] > 80) {
            result.put("alert", "CPU usage predicted to exceed 80%");
        } else if (metricType.contains("memory") && prediction[prediction.length - 1] > 85) {
            result.put("alert", "Memory usage predicted to exceed 85%");
        }
        
        return result;
    }

    /**
     * 获取性能瓶颈分析
     */
    @GetMapping("/bottleneck")
    @Operation(summary = "获取性能瓶颈分析")
    public Map<String, Object> analyzeBottleneck() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> bottlenecks = new ArrayList<>();
        
        // 获取当前指标
        Map<String, Object> currentMetrics = getRealtimeMetrics();
        
        // 分析CPU瓶颈
        Map<String, Object> cpu = (Map<String, Object>) currentMetrics.get("cpu");
        if (cpu != null) {
            Double cpuUsage = (Double) cpu.get("usage");
            if (cpuUsage != null && cpuUsage > 80) {
                Map<String, Object> bottleneck = new HashMap<>();
                bottleneck.put("type", "CPU");
                bottleneck.put("severity", cpuUsage > 95 ? "CRITICAL" : "WARNING");
                bottleneck.put("currentValue", String.format("%.2f%%", cpuUsage));
                bottleneck.put("threshold", "80%");
                bottleneck.put("recommendation", "Consider scaling up CPU resources or optimizing CPU-intensive operations");
                bottlenecks.add(bottleneck);
            }
        }
        
        // 分析内存瓶颈
        Map<String, Object> memory = (Map<String, Object>) currentMetrics.get("memory");
        if (memory != null) {
            Double memoryUsage = (Double) memory.get("usage");
            if (memoryUsage != null && memoryUsage > 85) {
                Map<String, Object> bottleneck = new HashMap<>();
                bottleneck.put("type", "Memory");
                bottleneck.put("severity", memoryUsage > 95 ? "CRITICAL" : "WARNING");
                bottleneck.put("currentValue", String.format("%.2f%%", memoryUsage));
                bottleneck.put("threshold", "85%");
                bottleneck.put("recommendation", "Consider increasing heap size or reviewing memory leaks");
                bottlenecks.add(bottleneck);
            }
        }
        
        // 分析GC瓶颈
        Map<String, Object> gc = (Map<String, Object>) currentMetrics.get("gc");
        if (gc != null) {
            Double gcTimePercent = (Double) gc.get("gcTimePercent");
            if (gcTimePercent != null && gcTimePercent > 10) {
                Map<String, Object> bottleneck = new HashMap<>();
                bottleneck.put("type", "GC");
                bottleneck.put("severity", gcTimePercent > 20 ? "CRITICAL" : "WARNING");
                bottleneck.put("currentValue", String.format("%.2f%%", gcTimePercent));
                bottleneck.put("threshold", "10%");
                bottleneck.put("recommendation", "Consider tuning GC parameters or reviewing object allocation patterns");
                bottlenecks.add(bottleneck);
            }
        }
        
        // 分析线程瓶颈
        Map<String, Object> threads = (Map<String, Object>) currentMetrics.get("threads");
        if (threads != null) {
            Integer deadlocked = (Integer) threads.get("deadlockedCount");
            if (deadlocked != null && deadlocked > 0) {
                Map<String, Object> bottleneck = new HashMap<>();
                bottleneck.put("type", "Thread");
                bottleneck.put("severity", "CRITICAL");
                bottleneck.put("currentValue", deadlocked + " threads");
                bottleneck.put("threshold", "0");
                bottleneck.put("recommendation", "Deadlock detected! Review thread synchronization and locking strategies");
                bottlenecks.add(bottleneck);
            }
        }
        
        result.put("bottlenecks", bottlenecks);
        result.put("bottleneckCount", bottlenecks.size());
        result.put("overallStatus", bottlenecks.isEmpty() ? "HEALTHY" : 
                (bottlenecks.stream().anyMatch(b -> "CRITICAL".equals(b.get("severity"))) ? "CRITICAL" : "WARNING"));
        result.put("timestamp", LocalDateTime.now().toString());
        
        return result;
    }

    /**
     * 获取性能对比
     */
    @GetMapping("/compare")
    @Operation(summary = "获取性能指标对比")
    public Map<String, Object> compareMetrics(
            @RequestParam(defaultValue = "60") int window1Minutes,
            @RequestParam(defaultValue = "60") int window2Minutes,
            @RequestParam(defaultValue = "60") int offsetMinutes) {
        Map<String, Object> result = new HashMap<>();
        
        LocalDateTime now = LocalDateTime.now();
        
        // 窗口1：最近
        LocalDateTime cutoff1 = now.minusMinutes(window1Minutes);
        List<Map<String, Object>> window1 = metricsHistory.stream()
                .filter(m -> {
                    String timestamp = (String) m.get("timestamp");
                    if (timestamp == null) return false;
                    LocalDateTime time = LocalDateTime.parse(timestamp);
                    return time.isAfter(cutoff1);
                })
                .collect(Collectors.toList());
        
        // 窗口2：之前
        LocalDateTime cutoff2Start = now.minusMinutes(offsetMinutes + window2Minutes);
        LocalDateTime cutoff2End = now.minusMinutes(offsetMinutes);
        List<Map<String, Object>> window2 = metricsHistory.stream()
                .filter(m -> {
                    String timestamp = (String) m.get("timestamp");
                    if (timestamp == null) return false;
                    LocalDateTime time = LocalDateTime.parse(timestamp);
                    return time.isAfter(cutoff2Start) && time.isBefore(cutoff2End);
                })
                .collect(Collectors.toList());
        
        Map<String, Object> comparison = new HashMap<>();
        comparison.put("window1", Map.of(
                "label", "Recent",
                "sampleCount", window1.size(),
                "durationMinutes", window1Minutes
        ));
        comparison.put("window2", Map.of(
                "label", "Previous",
                "sampleCount", window2.size(),
                "durationMinutes", window2Minutes,
                "offsetMinutes", offsetMinutes
        ));
        
        // 对比CPU
        comparison.put("cpu", compareMetric(window1, window2, "cpu", "usage"));
        
        // 对比内存
        comparison.put("memory", compareMetric(window1, window2, "memory", "usage"));
        
        result.put("comparison", comparison);
        result.put("timestamp", LocalDateTime.now().toString());
        
        return result;
    }

    // ==================== 私有方法 ====================

    private Map<String, Object> collectCpuMetrics() {
        Map<String, Object> cpu = new HashMap<>();
        
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        
        cpu.put("cores", osBean.getAvailableProcessors());
        cpu.put("loadAverage", osBean.getSystemLoadAverage());
        
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            cpu.put("usage", sunOsBean.getCpuLoad() * 100);
            cpu.put("systemUsage", sunOsBean.getSystemCpuLoad() * 100);
            cpu.put("processUsage", sunOsBean.getProcessCpuLoad() * 100);
        }
        
        return cpu;
    }

    private Map<String, Object> collectMemoryMetrics() {
        Map<String, Object> memory = new HashMap<>();
        
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        Map<String, Object> heap = new HashMap<>();
        heap.put("init", heapUsage.getInit());
        heap.put("used", heapUsage.getUsed());
        heap.put("committed", heapUsage.getCommitted());
        heap.put("max", heapUsage.getMax());
        heap.put("usage", heapUsage.getMax() > 0 ? (double) heapUsage.getUsed() / heapUsage.getMax() * 100 : 0);
        
        Map<String, Object> nonHeap = new HashMap<>();
        nonHeap.put("init", nonHeapUsage.getInit());
        nonHeap.put("used", nonHeapUsage.getUsed());
        nonHeap.put("committed", nonHeapUsage.getCommitted());
        nonHeap.put("max", nonHeapUsage.getMax());
        
        memory.put("heap", heap);
        memory.put("nonHeap", nonHeap);
        memory.put("usage", heap.get("usage"));
        
        return memory;
    }

    private Map<String, Object> collectGcMetrics() {
        Map<String, Object> gc = new HashMap<>();
        
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        
        long totalCount = 0;
        long totalTime = 0;
        
        List<Map<String, Object>> collectors = new ArrayList<>();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            Map<String, Object> collector = new HashMap<>();
            collector.put("name", gcBean.getName());
            collector.put("count", gcBean.getCollectionCount());
            collector.put("time", gcBean.getCollectionTime());
            collectors.add(collector);
            
            totalCount += gcBean.getCollectionCount();
            totalTime += gcBean.getCollectionTime();
        }
        
        gc.put("collectors", collectors);
        gc.put("totalCount", totalCount);
        gc.put("totalTime", totalTime);
        
        // 计算GC时间占比（简化）
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long uptime = runtimeBean.getUptime();
        gc.put("gcTimePercent", uptime > 0 ? (double) totalTime / uptime * 100 : 0);
        
        return gc;
    }

    private Map<String, Object> collectThreadMetrics() {
        Map<String, Object> threads = new HashMap<>();
        
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        
        threads.put("count", threadBean.getThreadCount());
        threads.put("peakCount", threadBean.getPeakThreadCount());
        threads.put("daemonCount", threadBean.getDaemonThreadCount());
        threads.put("totalStarted", threadBean.getTotalStartedThreadCount());
        
        long[] deadlocked = threadBean.findDeadlockedThreads();
        threads.put("deadlockedCount", deadlocked != null ? deadlocked.length : 0);
        
        return threads;
    }

    private Map<String, Object> collectClassLoadingMetrics() {
        Map<String, Object> classLoading = new HashMap<>();
        
        ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();
        
        classLoading.put("loadedClassCount", classLoadingBean.getLoadedClassCount());
        classLoading.put("totalLoadedClassCount", classLoadingBean.getTotalLoadedClassCount());
        classLoading.put("unloadedClassCount", classLoadingBean.getUnloadedClassCount());
        
        return classLoading;
    }

    private Map<String, Object> collectCompilationMetrics() {
        Map<String, Object> compilation = new HashMap<>();
        
        CompilationMXBean compilationBean = ManagementFactory.getCompilationMXBean();
        
        compilation.put("name", compilationBean.getName());
        compilation.put("totalCompilationTime", compilationBean.getTotalCompilationTime());
        
        return compilation;
    }

    private void saveMetricsHistory(Map<String, Object> metrics) {
        synchronized (metricsHistory) {
            metricsHistory.add(new HashMap<>(metrics));
            if (metricsHistory.size() > MAX_HISTORY_SIZE) {
                metricsHistory.remove(0);
            }
        }
    }

    private Map<String, Object> aggregateCpuMetrics(List<Map<String, Object>> history) {
        List<Double> usages = extractValues(history, "cpu", "usage");
        
        Map<String, Object> aggregate = new HashMap<>();
        aggregate.put("avgUsage", usages.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        aggregate.put("maxUsage", usages.stream().max(Double::compare).orElse(0.0));
        aggregate.put("minUsage", usages.stream().min(Double::compare).orElse(0.0));
        
        return aggregate;
    }

    private Map<String, Object> aggregateMemoryMetrics(List<Map<String, Object>> history) {
        List<Double> usages = extractValues(history, "memory", "usage");
        
        Map<String, Object> aggregate = new HashMap<>();
        aggregate.put("avgUsage", usages.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        aggregate.put("maxUsage", usages.stream().max(Double::compare).orElse(0.0));
        aggregate.put("minUsage", usages.stream().min(Double::compare).orElse(0.0));
        
        return aggregate;
    }

    private Map<String, Object> aggregateGcMetrics(List<Map<String, Object>> history) {
        List<Double> gcTimes = extractValues(history, "gc", "gcTimePercent");
        
        Map<String, Object> aggregate = new HashMap<>();
        aggregate.put("avgGcTimePercent", gcTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        aggregate.put("maxGcTimePercent", gcTimes.stream().max(Double::compare).orElse(0.0));
        
        return aggregate;
    }

    private List<Double> extractValues(List<Map<String, Object>> history, String category, String metric) {
        return history.stream()
                .map(h -> {
                    Map<String, Object> cat = (Map<String, Object>) h.get(category);
                    if (cat == null) return null;
                    Object val = cat.get(metric);
                    if (val instanceof Number) {
                        return ((Number) val).doubleValue();
                    }
                    return null;
                })
                .filter(v -> v != null)
                .collect(Collectors.toList());
    }

    private Double extractMetricValue(Map<String, Object> metrics, String metricType) {
        String[] parts = metricType.split("\\.");
        if (parts.length == 2) {
            Map<String, Object> category = (Map<String, Object>) metrics.get(parts[0]);
            if (category != null) {
                Object val = category.get(parts[1]);
                if (val instanceof Number) {
                    return ((Number) val).doubleValue();
                }
            }
        }
        return null;
    }

    private double calculateTrend(List<Double> values) {
        if (values.size() < 2) return 0;
        
        // 简单线性回归斜率
        int n = values.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values.get(i);
            sumXY += i * values.get(i);
            sumX2 += i * i;
        }
        
        return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
    }

    private double[] linearRegressionPredict(List<Double> values, int steps) {
        int n = values.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values.get(i);
            sumXY += i * values.get(i);
            sumX2 += i * i;
        }
        
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;
        
        double[] prediction = new double[steps];
        for (int i = 0; i < steps; i++) {
            prediction[i] = intercept + slope * (n + i);
        }
        
        return prediction;
    }

    private double calculateConfidence(List<Double> values) {
        if (values.size() < 10) return 0.5;
        
        // 基于方差的简单置信度计算
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);
        double stdDev = Math.sqrt(variance);
        
        // 变异系数越小，置信度越高
        double cv = mean > 0 ? stdDev / mean : 1;
        return Math.max(0, Math.min(1, 1 - cv));
    }

    private Map<String, Object> compareMetric(List<Map<String, Object>> window1, 
                                               List<Map<String, Object>> window2,
                                               String category, String metric) {
        List<Double> values1 = extractValues(window1, category, metric);
        List<Double> values2 = extractValues(window2, category, metric);
        
        double avg1 = values1.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double avg2 = values2.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        
        Map<String, Object> result = new HashMap<>();
        result.put("window1Avg", avg1);
        result.put("window2Avg", avg2);
        result.put("change", avg1 - avg2);
        result.put("changePercent", avg2 > 0 ? (avg1 - avg2) / avg2 * 100 : 0);
        result.put("trend", avg1 > avg2 ? "increasing" : (avg1 < avg2 ? "decreasing" : "stable"));
        
        return result;
    }
}