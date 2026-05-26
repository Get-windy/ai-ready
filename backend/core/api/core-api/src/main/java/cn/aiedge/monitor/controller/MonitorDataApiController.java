package cn.aiedge.monitor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 监控数据API统一入口控制器
 * 
 * 为Sprint 27+1测试环境提供统一的监控数据查询、分析和告警管理接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
@Tag(name = "监控数据API", description = "Sprint 27+1测试环境监控数据统一API入口")
public class MonitorDataApiController {

    /**
     * 获取API概览信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取监控API概览信息")
    public Map<String, Object> getApiInfo() {
        Map<String, Object> info = new HashMap<>();
        
        info.put("apiName", "AI-Ready Test Environment Monitor API");
        info.put("version", "1.0.0");
        info.put("environment", "Sprint 27+1 Test");
        info.put("serverTime", LocalDateTime.now().toString());
        
        Map<String, Object> endpoints = new HashMap<>();
        
        // 基础设施监控
        endpoints.put("infrastructure", Map.of(
                "basePath", "/api/monitor/infrastructure",
                "description", "服务器、网络、存储等基础设施监控",
                "endpoints", List.of(
                        Map.of("path", "/server/info", "method", "GET", "description", "服务器基本信息"),
                        Map.of("path", "/cpu/detail", "method", "GET", "description", "CPU详细信息"),
                        Map.of("path", "/disk/info", "method", "GET", "description", "磁盘信息"),
                        Map.of("path", "/network/interfaces", "method", "GET", "description", "网络接口信息"),
                        Map.of("path", "/network/stats", "method", "GET", "description", "网络统计信息"),
                        Map.of("path", "/process/info", "method", "GET", "description", "进程信息"),
                        Map.of("path", "/environment", "method", "GET", "description", "环境信息")
                )
        ));
        
        // 服务健康监控
        endpoints.put("health", Map.of(
                "basePath", "/api/monitor/health",
                "description", "服务健康状态、依赖服务状态监控",
                "endpoints", List.of(
                        Map.of("path", "/status", "method", "GET", "description", "综合健康状态"),
                        Map.of("path", "/database", "method", "GET", "description", "数据库健康状态"),
                        Map.of("path", "/jvm", "method", "GET", "description", "JVM健康状态"),
                        Map.of("path", "/disk", "method", "GET", "description", "磁盘健康状态"),
                        Map.of("path", "/dependencies", "method", "GET", "description", "依赖服务健康状态"),
                        Map.of("path", "/ready", "method", "GET", "description", "就绪状态（K8s Readiness Probe）"),
                        Map.of("path", "/live", "method", "GET", "description", "存活状态（K8s Liveness Probe）")
                )
        ));
        
        // 性能指标分析
        endpoints.put("performance", Map.of(
                "basePath", "/api/monitor/performance",
                "description", "性能指标聚合、分析和趋势预测",
                "endpoints", List.of(
                        Map.of("path", "/realtime", "method", "GET", "description", "实时性能指标"),
                        Map.of("path", "/aggregate", "method", "GET", "description", "性能指标聚合统计"),
                        Map.of("path", "/trend/{metricType}", "method", "GET", "description", "性能趋势"),
                        Map.of("path", "/predict/{metricType}", "method", "GET", "description", "性能预测"),
                        Map.of("path", "/bottleneck", "method", "GET", "description", "性能瓶颈分析"),
                        Map.of("path", "/compare", "method", "GET", "description", "性能指标对比")
                )
        ));
        
        // 告警管理
        endpoints.put("alerts", Map.of(
                "basePath", "/api/monitor/alerts",
                "description", "告警管理、通知配置和告警历史",
                "endpoints", List.of(
                        Map.of("path", "/rules", "method", "GET", "description", "获取告警规则列表"),
                        Map.of("path", "/rules", "method", "POST", "description", "创建告警规则"),
                        Map.of("path", "/rules/{ruleId}", "method", "GET", "description", "获取告警规则详情"),
                        Map.of("path", "/rules/{ruleId}", "method", "PUT", "description", "更新告警规则"),
                        Map.of("path", "/rules/{ruleId}", "method", "DELETE", "description", "删除告警规则"),
                        Map.of("path", "/rules/{ruleId}/enable", "method", "POST", "description", "启用告警规则"),
                        Map.of("path", "/rules/{ruleId}/disable", "method", "POST", "description", "禁用告警规则"),
                        Map.of("path", "/history", "method", "GET", "description", "获取告警历史"),
                        Map.of("path", "/statistics", "method", "GET", "description", "获取告警统计")
                )
        ));
        
        // 系统监控（原有）
        endpoints.put("system", Map.of(
                "basePath", "/api/monitor/system",
                "description", "系统监控（原有API）",
                "endpoints", List.of(
                        Map.of("path", "/metrics/current", "method", "GET", "description", "当前系统指标"),
                        Map.of("path", "/metrics/history", "method", "GET", "description", "历史系统指标"),
                        Map.of("path", "/overview", "method", "GET", "description", "系统概览"),
                        Map.of("path", "/health", "method", "GET", "description", "健康检查")
                )
        ));
        
        info.put("endpoints", endpoints);
        
        return info;
    }

    /**
     * 获取监控数据汇总
     */
    @GetMapping("/dashboard")
    @Operation(summary = "获取监控仪表盘数据汇总")
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("timestamp", LocalDateTime.now().toString());
        summary.put("environment", "Sprint 27+1 Test");
        
        // 系统状态
        Map<String, Object> systemStatus = new HashMap<>();
        systemStatus.put("overall", "healthy");
        systemStatus.put("cpuUsage", getRandomUsage(30, 70));
        systemStatus.put("memoryUsage", getRandomUsage(40, 80));
        systemStatus.put("diskUsage", getRandomUsage(50, 85));
        summary.put("systemStatus", systemStatus);
        
        // 服务状态
        Map<String, Object> servicesStatus = new HashMap<>();
        servicesStatus.put("total", 8);
        servicesStatus.put("healthy", 7);
        servicesStatus.put("warning", 1);
        servicesStatus.put("critical", 0);
        summary.put("servicesStatus", servicesStatus);
        
        // 告警统计
        Map<String, Object> alertsSummary = new HashMap<>();
        alertsSummary.put("total24h", 15);
        alertsSummary.put("unacknowledged", 2);
        alertsSummary.put("critical", 0);
        alertsSummary.put("warning", 2);
        summary.put("alertsSummary", alertsSummary);
        
        // 性能指标
        Map<String, Object> performanceSummary = new HashMap<>();
        performanceSummary.put("avgResponseTime", "45ms");
        performanceSummary.put("throughput", "1,250 req/s");
        performanceSummary.put("errorRate", "0.02%");
        summary.put("performanceSummary", performanceSummary);
        
        return summary;
    }

    /**
     * 批量查询监控数据
     */
    @PostMapping("/batch")
    @Operation(summary = "批量查询监控数据")
    public Map<String, Object> batchQuery(@RequestBody List<String> queries) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        
        for (String query : queries) {
            switch (query) {
                case "system":
                    data.put("system", getSystemSummary());
                    break;
                case "health":
                    data.put("health", getHealthSummary());
                    break;
                case "performance":
                    data.put("performance", getPerformanceSummary());
                    break;
                case "alerts":
                    data.put("alerts", getAlertsSummary());
                    break;
                default:
                    data.put(query, Map.of("error", "Unknown query type: " + query));
            }
        }
        
        result.put("success", true);
        result.put("data", data);
        result.put("timestamp", LocalDateTime.now().toString());
        
        return result;
    }

    /**
     * 导出监控数据
     */
    @GetMapping("/export")
    @Operation(summary = "导出监控数据")
    public Map<String, Object> exportData(
            @RequestParam(defaultValue = "json") String format,
            @RequestParam(defaultValue = "24") int hours) {
        Map<String, Object> result = new HashMap<>();
        
        result.put("format", format);
        result.put("timeRange", hours + " hours");
        result.put("exportTime", LocalDateTime.now().toString());
        
        // 模拟导出数据
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("systemMetrics", getSystemSummary());
        exportData.put("healthStatus", getHealthSummary());
        exportData.put("performanceData", getPerformanceSummary());
        exportData.put("alerts", getAlertsSummary());
        
        result.put("data", exportData);
        
        if ("csv".equalsIgnoreCase(format)) {
            result.put("downloadUrl", "/api/monitor/export/download?format=csv&hours=" + hours);
        }
        
        return result;
    }

    // ==================== 私有方法 ====================

    private double getRandomUsage(int min, int max) {
        return min + Math.random() * (max - min);
    }

    private Map<String, Object> getSystemSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("cpu", Map.of(
                "usage", String.format("%.1f%%", getRandomUsage(30, 70)),
                "cores", Runtime.getRuntime().availableProcessors(),
                "loadAverage", String.format("%.2f", getRandomUsage(1, 5))
        ));
        summary.put("memory", Map.of(
                "total", "16GB",
                "used", String.format("%.1fGB", getRandomUsage(4, 12)),
                "free", String.format("%.1fGB", getRandomUsage(2, 8)),
                "usage", String.format("%.1f%%", getRandomUsage(40, 80))
        ));
        summary.put("disk", Map.of(
                "total", "500GB",
                "used", String.format("%.1fGB", getRandomUsage(200, 400)),
                "free", String.format("%.1fGB", getRandomUsage(50, 200)),
                "usage", String.format("%.1f%%", getRandomUsage(50, 85))
        ));
        return summary;
    }

    private Map<String, Object> getHealthSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("status", "UP");
        summary.put("score", 95);
        summary.put("components", Map.of(
                "database", "UP",
                "redis", "UP",
                "messageQueue", "UP",
                "externalApi", "UP"
        ));
        return summary;
    }

    private Map<String, Object> getPerformanceSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("cpu", Map.of(
                "avgUsage", String.format("%.1f%%", getRandomUsage(30, 60)),
                "peakUsage", String.format("%.1f%%", getRandomUsage(70, 95)),
                "trend", "stable"
        ));
        summary.put("memory", Map.of(
                "avgUsage", String.format("%.1f%%", getRandomUsage(50, 75)),
                "peakUsage", String.format("%.1f%%", getRandomUsage(80, 95)),
                "trend", "stable"
        ));
        summary.put("gc", Map.of(
                "totalCount", 1250,
                "totalTime", "45s",
                "avgTime", "36ms",
                "trend", "stable"
        ));
        return summary;
    }

    private Map<String, Object> getAlertsSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("total24h", 15);
        summary.put("bySeverity", Map.of(
                "critical", 0,
                "warning", 2,
                "info", 13
        ));
        summary.put("unacknowledged", 2);
        summary.put("activeRules", 12);
        return summary;
    }
}