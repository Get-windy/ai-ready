package com.qizhilian.monitoring.controller;

import com.qizhilian.monitoring.entity.MetricEntity;
import com.qizhilian.monitoring.model.dto.MetricDTO;
import com.qizhilian.monitoring.service.IMetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 监控指标采集API控制器
 * 提供30+系统/应用/业务指标的采集和查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
public class MetricController {

    private final IMetricService metricService;

    @PostMapping
    public ResponseEntity<MetricEntity> saveMetric(@RequestBody MetricDTO dto) {
        return ResponseEntity.ok(metricService.saveMetric(dto));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<MetricEntity>> saveMetrics(@RequestBody List<MetricDTO> dtos) {
        return ResponseEntity.ok(metricService.saveMetrics(dtos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MetricEntity> getMetric(@PathVariable Long id) {
        return ResponseEntity.ok(metricService.getMetricById(id));
    }

    @GetMapping
    public ResponseEntity<Page<MetricEntity>> searchMetrics(
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) String metricType,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(metricService.searchMetrics(metricName, metricType, startTime, endTime, pageable));
    }

    @GetMapping("/name/{metricName}")
    public ResponseEntity<List<MetricEntity>> getByName(@PathVariable String metricName) {
        return ResponseEntity.ok(metricService.getMetricsByName(metricName));
    }

    @GetMapping("/type/{metricType}")
    public ResponseEntity<List<MetricEntity>> getByType(@PathVariable String metricType) {
        return ResponseEntity.ok(metricService.getMetricsByType(metricType));
    }

    @GetMapping("/latest/{metricName}")
    public ResponseEntity<List<MetricEntity>> getLatest(@PathVariable String metricName,
                                                        @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(metricService.getLatestMetrics(metricName, limit));
    }

    @GetMapping("/statistics/{metricName}")
    public ResponseEntity<Map<String, Object>> getStatistics(@PathVariable String metricName,
                                                              @RequestParam LocalDateTime startTime,
                                                              @RequestParam LocalDateTime endTime) {
        return ResponseEntity.ok(metricService.calculateMetricStatistics(metricName, startTime, endTime));
    }

    @GetMapping("/trend/{metricName}")
    public ResponseEntity<List<Map<String, Object>>> getTrend(@PathVariable String metricName,
                                                               @RequestParam LocalDateTime startTime,
                                                               @RequestParam LocalDateTime endTime,
                                                               @RequestParam(defaultValue = "5") int intervalMinutes) {
        return ResponseEntity.ok(metricService.getMetricTrend(metricName, startTime, endTime, intervalMinutes));
    }

    @GetMapping("/snapshot/system")
    public ResponseEntity<Map<String, Object>> getSystemSnapshot(@RequestParam String hostname) {
        return ResponseEntity.ok(metricService.getSystemMetricsSnapshot(hostname));
    }

    @GetMapping("/snapshot/application")
    public ResponseEntity<Map<String, Object>> getApplicationSnapshot(@RequestParam String serviceName) {
        return ResponseEntity.ok(metricService.getApplicationMetricsSnapshot(serviceName));
    }

    @GetMapping("/snapshot/business")
    public ResponseEntity<Map<String, Object>> getBusinessSnapshot(@RequestParam String businessCode) {
        return ResponseEntity.ok(metricService.getBusinessMetricsSnapshot(businessCode));
    }

    @GetMapping("/names")
    public ResponseEntity<List<String>> getAllNames() {
        return ResponseEntity.ok(metricService.getAllMetricNames());
    }

    @GetMapping("/types")
    public ResponseEntity<List<String>> getAllTypes() {
        return ResponseEntity.ok(metricService.getAllMetricTypes());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMetric(@PathVariable Long id) {
        metricService.deleteMetric(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/batch")
    public ResponseEntity<Void> deleteMetrics(@RequestBody List<Long> ids) {
        metricService.deleteMetrics(ids);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/expired")
    public ResponseEntity<Integer> deleteExpired(@RequestParam(defaultValue = "30") int retentionDays) {
        return ResponseEntity.ok(metricService.deleteExpiredMetrics(retentionDays));
    }

    @GetMapping("/count/stats")
    public ResponseEntity<Map<String, Long>> getCountStats() {
        return ResponseEntity.ok(metricService.getMetricCountStatistics());
    }

    /**
     * 批量采集系统指标（30+指标统一入口）
     */
    @PostMapping("/collect/system")
    public ResponseEntity<List<MetricEntity>> collectSystemMetrics(@RequestBody Map<String, Object> payload) {
        String hostname = (String) payload.get("hostname");
        Map<String, Object> metrics = (Map<String, Object>) payload.get("metrics");
        List<MetricDTO> dtos = new ArrayList<>();

        // 系统CPU指标
        addMetric(dtos, "system.cpu.usage", "system.cpu", metrics.get("cpuUsage"), "%", hostname, "system");
        addMetric(dtos, "system.cpu.user", "system.cpu", metrics.get("cpuUser"), "%", hostname, "system");
        addMetric(dtos, "system.cpu.system", "system.cpu", metrics.get("cpuSystem"), "%", hostname, "system");
        addMetric(dtos, "system.cpu.iowait", "system.cpu", metrics.get("cpuIoWait"), "%", hostname, "system");
        addMetric(dtos, "system.cpu.load.average", "system.cpu", metrics.get("loadAverage"), "", hostname, "system");

        // 系统内存指标
        addMetric(dtos, "system.memory.usage", "system.memory", metrics.get("memUsage"), "%", hostname, "system");
        addMetric(dtos, "system.memory.used", "system.memory", metrics.get("memUsed"), "MB", hostname, "system");
        addMetric(dtos, "system.memory.free", "system.memory", metrics.get("memFree"), "MB", hostname, "system");
        addMetric(dtos, "system.memory.total", "system.memory", metrics.get("memTotal"), "MB", hostname, "system");
        addMetric(dtos, "system.memory.cache", "system.memory", metrics.get("memCache"), "MB", hostname, "system");
        addMetric(dtos, "system.memory.buffer", "system.memory", metrics.get("memBuffer"), "MB", hostname, "system");
        addMetric(dtos, "system.swap.usage", "system.memory", metrics.get("swapUsage"), "%", hostname, "system");
        addMetric(dtos, "system.swap.used", "system.memory", metrics.get("swapUsed"), "MB", hostname, "system");

        // 系统磁盘指标
        addMetric(dtos, "system.disk.usage", "system.disk", metrics.get("diskUsage"), "%", hostname, "system");
        addMetric(dtos, "system.disk.used", "system.disk", metrics.get("diskUsed"), "GB", hostname, "system");
        addMetric(dtos, "system.disk.free", "system.disk", metrics.get("diskFree"), "GB", hostname, "system");
        addMetric(dtos, "system.disk.total", "system.disk", metrics.get("diskTotal"), "GB", hostname, "system");
        addMetric(dtos, "system.disk.read.bytes", "system.disk", metrics.get("diskReadBytes"), "KB/s", hostname, "system");
        addMetric(dtos, "system.disk.write.bytes", "system.disk", metrics.get("diskWriteBytes"), "KB/s", hostname, "system");
        addMetric(dtos, "system.disk.iops", "system.disk", metrics.get("diskIops"), "iops", hostname, "system");
        addMetric(dtos, "system.inodes.usage", "system.disk", metrics.get("inodesUsage"), "%", hostname, "system");

        // 系统网络指标
        addMetric(dtos, "system.network.bytes.in", "system.network", metrics.get("netBytesIn"), "KB/s", hostname, "system");
        addMetric(dtos, "system.network.bytes.out", "system.network", metrics.get("netBytesOut"), "KB/s", hostname, "system");
        addMetric(dtos, "system.network.packets.in", "system.network", metrics.get("netPacketsIn"), "pps", hostname, "system");
        addMetric(dtos, "system.network.packets.out", "system.network", metrics.get("netPacketsOut"), "pps", hostname, "system");
        addMetric(dtos, "system.network.errors.in", "system.network", metrics.get("netErrorsIn"), "count/s", hostname, "system");
        addMetric(dtos, "system.network.errors.out", "system.network", metrics.get("netErrorsOut"), "count/s", hostname, "system");
        addMetric(dtos, "system.tcp.connections", "system.network", metrics.get("tcpConnections"), "count", hostname, "system");
        addMetric(dtos, "system.tcp.connections.established", "system.network", metrics.get("tcpEstablished"), "count", hostname, "system");

        // 系统进程指标
        addMetric(dtos, "system.processes.total", "system.process", metrics.get("processTotal"), "count", hostname, "system");
        addMetric(dtos, "system.processes.running", "system.process", metrics.get("processRunning"), "count", hostname, "system");
        addMetric(dtos, "system.processes.zombie", "system.process", metrics.get("processZombie"), "count", hostname, "system");
        addMetric(dtos, "system.processes.blocked", "system.process", metrics.get("processBlocked"), "count", hostname, "system");
        addMetric(dtos, "system.threads.total", "system.process", metrics.get("threadsTotal"), "count", hostname, "system");
        addMetric(dtos, "system.open.files", "system.process", metrics.get("openFiles"), "count", hostname, "system");

        // 系统文件描述符
        addMetric(dtos, "system.fd.usage", "system.fd", metrics.get("fdUsage"), "%", hostname, "system");
        addMetric(dtos, "system.fd.used", "system.fd", metrics.get("fdUsed"), "count", hostname, "system");
        addMetric(dtos, "system.fd.max", "system.fd", metrics.get("fdMax"), "count", hostname, "system");

        return ResponseEntity.ok(metricService.saveMetrics(dtos));
    }

    /**
     * 批量采集应用指标
     */
    @PostMapping("/collect/application")
    public ResponseEntity<List<MetricEntity>> collectApplicationMetrics(@RequestBody Map<String, Object> payload) {
        String serviceName = (String) payload.get("serviceName");
        String hostname = (String) payload.get("hostname");
        Map<String, Object> metrics = (Map<String, Object>) payload.get("metrics");
        List<MetricDTO> dtos = new ArrayList<>();

        addMetric(dtos, "application.jvm.memory.used", "application.jvm", metrics.get("jvmMemUsed"), "MB", hostname, serviceName);
        addMetric(dtos, "application.jvm.memory.max", "application.jvm", metrics.get("jvmMemMax"), "MB", hostname, serviceName);
        addMetric(dtos, "application.jvm.memory.heap.used", "application.jvm", metrics.get("jvmHeapUsed"), "MB", hostname, serviceName);
        addMetric(dtos, "application.jvm.gc.count", "application.jvm", metrics.get("jvmGcCount"), "count/min", hostname, serviceName);
        addMetric(dtos, "application.jvm.gc.time", "application.jvm", metrics.get("jvmGcTime"), "ms", hostname, serviceName);
        addMetric(dtos, "application.jvm.threads.count", "application.jvm", metrics.get("jvmThreads"), "count", hostname, serviceName);
        addMetric(dtos, "application.jvm.classes.loaded", "application.jvm", metrics.get("jvmClassesLoaded"), "count", hostname, serviceName);

        addMetric(dtos, "application.http.requests", "application.http", metrics.get("httpRequests"), "req/s", hostname, serviceName);
        addMetric(dtos, "application.http.response.time", "application.http", metrics.get("httpResponseTime"), "ms", hostname, serviceName);
        addMetric(dtos, "application.http.errors", "application.http", metrics.get("httpErrors"), "count/s", hostname, serviceName);
        addMetric(dtos, "application.http.status.4xx", "application.http", metrics.get("http4xx"), "count/s", hostname, serviceName);
        addMetric(dtos, "application.http.status.5xx", "application.http", metrics.get("http5xx"), "count/s", hostname, serviceName);

        addMetric(dtos, "application.db.connections.active", "application.db", metrics.get("dbConnActive"), "count", hostname, serviceName);
        addMetric(dtos, "application.db.connections.max", "application.db", metrics.get("dbConnMax"), "count", hostname, serviceName);
        addMetric(dtos, "application.db.query.time", "application.db", metrics.get("dbQueryTime"), "ms", hostname, serviceName);
        addMetric(dtos, "application.db.query.errors", "application.db", metrics.get("dbQueryErrors"), "count/s", hostname, serviceName);
        addMetric(dtos, "application.db.transactions", "application.db", metrics.get("dbTransactions"), "count/s", hostname, serviceName);

        addMetric(dtos, "application.cache.hit.rate", "application.cache", metrics.get("cacheHitRate"), "%", hostname, serviceName);
        addMetric(dtos, "application.cache.miss.rate", "application.cache", metrics.get("cacheMissRate"), "%", hostname, serviceName);
        addMetric(dtos, "application.cache.evictions", "application.cache", metrics.get("cacheEvictions"), "count/s", hostname, serviceName);

        addMetric(dtos, "application.message.queue.lag", "application.mq", metrics.get("mqLag"), "count", hostname, serviceName);
        addMetric(dtos, "application.message.queue.produce.rate", "application.mq", metrics.get("mqProduceRate"), "msg/s", hostname, serviceName);
        addMetric(dtos, "application.message.queue.consume.rate", "application.mq", metrics.get("mqConsumeRate"), "msg/s", hostname, serviceName);

        addMetric(dtos, "application.error.rate", "application.error", metrics.get("errorRate"), "%", hostname, serviceName);
        addMetric(dtos, "application.error.count", "application.error", metrics.get("errorCount"), "count/s", hostname, serviceName);
        addMetric(dtos, "application.log.errors", "application.error", metrics.get("logErrors"), "count/s", hostname, serviceName);
        addMetric(dtos, "application.log.warns", "application.error", metrics.get("logWarns"), "count/s", hostname, serviceName);

        return ResponseEntity.ok(metricService.saveMetrics(dtos));
    }

    /**
     * 批量采集业务指标
     */
    @PostMapping("/collect/business")
    public ResponseEntity<List<MetricEntity>> collectBusinessMetrics(@RequestBody Map<String, Object> payload) {
        String businessCode = (String) payload.get("businessCode");
        Map<String, Object> metrics = (Map<String, Object>) payload.get("metrics");
        List<MetricDTO> dtos = new ArrayList<>();

        addMetric(dtos, "business.user.active", "business.user", metrics.get("activeUsers"), "count", null, businessCode);
        addMetric(dtos, "business.user.new", "business.user", metrics.get("newUsers"), "count", null, businessCode);
        addMetric(dtos, "business.user.total", "business.user", metrics.get("totalUsers"), "count", null, businessCode);
        addMetric(dtos, "business.order.count", "business.order", metrics.get("orderCount"), "count", null, businessCode);
        addMetric(dtos, "business.order.amount", "business.order", metrics.get("orderAmount"), "CNY", null, businessCode);
        addMetric(dtos, "business.payment.success.rate", "business.payment", metrics.get("paymentSuccessRate"), "%", null, businessCode);
        addMetric(dtos, "business.payment.amount", "business.payment", metrics.get("paymentAmount"), "CNY", null, businessCode);
        addMetric(dtos, "business.conversion.rate", "business.conversion", metrics.get("conversionRate"), "%", null, businessCode);
        addMetric(dtos, "business.api.qps", "business.api", metrics.get("apiQps"), "req/s", null, businessCode);
        addMetric(dtos, "business.api.latency.p99", "business.api", metrics.get("apiLatencyP99"), "ms", null, businessCode);

        return ResponseEntity.ok(metricService.saveMetrics(dtos));
    }

    private void addMetric(List<MetricDTO> list, String name, String type, Object value, String unit, String hostname, String service) {
        if (value == null) return;
        try {
            MetricDTO dto = new MetricDTO();
            dto.setMetricName(name);
            dto.setMetricType(type);
            dto.setMetricValue(new BigDecimal(value.toString()));
            dto.setMetricUnit(unit);
            dto.setSourceType("agent");
            dto.setHostname(hostname);
            dto.setServiceName(service);
            dto.setEnvironment("prod");
            dto.setMetricTime(LocalDateTime.now());
            list.add(dto);
        } catch (Exception e) {
            log.warn("Failed to parse metric {}: {}", name, value);
        }
    }
}
