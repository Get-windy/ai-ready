package com.qizhilian.api.monitor;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;

/**
 * 监控指标采集测试
 * 验证监控系统能够正确采集各项指标数据
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Epic("监控告警系统")
@Feature("监控指标采集")
@DisplayName("监控指标采集测试套件")
public class MonitorMetricsTest extends MonitorBaseTest {
    
    // ==================== 系统资源监控测试 ====================
    
    @Test
    @Order(1)
    @Story("系统资源监控")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("METRIC-SYS-001: CPU使用率采集测试")
    void testCpuUsageCollection() {
        Response response = getSystemMetrics("/monitor/metrics/cpu");
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.usage", notNullValue())
            .body("data.usage", greaterthan(0.0))
            .body("data.usage", lessthan(100.0))
            .body("data.timestamp", notNullValue())
            .body("data.unit", equalTo("percent"));
        
        log.info("CPU使用率: {}%", response.jsonPath().getDouble("data.usage"));
    }
    
    @Test
    @Order(2)
    @Story("系统资源监控")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("METRIC-SYS-002: 内存使用率采集测试")
    void testMemoryUsageCollection() {
        Response response = getSystemMetrics("/monitor/metrics/memory");
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.usage", notNullValue())
            .body("data.usage", greaterthan(0.0))
            .body("data.usage", lessthan(100.0))
            .body("data.total", notNullValue())
            .body("data.used", notNullValue())
            .body("data.free", notNullValue())
            .body("data.unit", equalTo("bytes"));
        
        log.info("内存使用率: {}%", response.jsonPath().getDouble("data.usage"));
    }
    
    @Test
    @Order(3)
    @Story("系统资源监控")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("METRIC-SYS-003: 磁盘使用率采集测试")
    void testDiskUsageCollection() {
        Response response = getSystemMetrics("/monitor/metrics/disk");
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.usage", notNullValue())
            .body("data.usage", greaterthan(0.0))
            .body("data.usage", lessthan(100.0))
            .body("data.total", notNullValue())
            .body("data.used", notNullValue())
            .body("data.free", notNullValue());
        
        log.info("磁盘使用率: {}%", response.jsonPath().getDouble("data.usage));
    }
    
    @Test
    @Order(4)
    @Story("系统资源监控")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("METRIC-SYS-004: 网络流量监控测试")
    void testNetworkTrafficCollection() {
        Response response = getSystemMetrics("/monitor/metrics/network");
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.bytesSent", notNullValue())
            .body("data.bytesReceived", notNullValue())
            .body("data.packetsSent", notNullValue())
            .body("data.packetsReceived", notNullValue());
        
        log.info("网络流量: 发送={}字节, 接收={}字节", 
            response.jsonPath().getLong("data.bytesSent"),
            response.jsonPath().getLong("data.bytesReceived"));
    }
    
    // ==================== 应用性能监控测试 ====================
    
    @Test
    @Order(10)
    @Story("应用性能监控")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("METRIC-APP-001: JVM状态监控测试")
    void testJvmStatusCollection() {
        Response response = getJvmStatus();
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.heapUsage", notNullValue())
            .body("data.nonHeapUsage", notNullValue())
            .body("data.threadCount", notNullValue())
            .body("data.gcCount", notNullValue())
            .body("data.gcTime", notNullValue());
        
        log.info("JVM堆内存使用率: {}%", response.jsonPath().getDouble("data.heapUsage"));
    }
    
    @Test
    @Order(11)
    @Story("应用性能监控")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("METRIC-APP-002: 数据库连接池监控测试")
    void testDbPoolStatusCollection() {
        Response response = getDbPoolStatus();
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.activeConnections", notNullValue())
            .body("data.idleConnections", notNullValue())
            .body("data.maxConnections", notNullValue())
            .body("data.minConnections", notNullValue())
            .body("data.waitingThreads", notNullValue());
        
        log.info("数据库连接池: 活跃={}个, 空闲={}个", 
            response.jsonPath().getInt("data.activeConnections"),
            response.jsonPath().getInt("data.idleConnections"));
    }
    
    @Test
    @Order(12)
    @Story("应用性能监控")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("METRIC-APP-003: HTTP请求监控测试")
    void testHttpRequestCollection() {
        Response response = getSystemMetrics("/monitor/metrics/http");
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.requestCount", notNullValue())
            .body("data.errorCount", notNullValue())
            .body("data.avgResponseTime", notNullValue())
            .body("data.maxResponseTime", notNullValue());
        
        log.info("HTTP请求: 总数={}, 错误数={}, 平均响应时间={}ms", 
            response.jsonPath().getInt("data.requestCount"),
            response.jsonPath().getInt("data.errorCount"),
            response.jsonPath().getInt("data.avgResponseTime"));
    }
    
    // ==================== 自定义指标监控测试 ====================
    
    @Test
    @Order(20)
    @Story("自定义指标监控")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("METRIC-CUST-001: 自定义业务指标测试")
    void testCustomMetrics() {
        // 测试自定义指标上报
        Map<String, Object> metric = new HashMap<>();
        metric.put("name", "test_business_metric");
        metric.put("value", 123.45);
        metric.put("tags", new String[]{"env:test", "service:api"});
        
        Response reportResponse = post("/monitor/metrics/report", metric);
        assertSuccess(reportResponse);
        
        // 等待指标持久化
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 查询指标
        Response queryResponse = get("/monitor/metrics/custom?name=test_business_metric");
        assertSuccess(queryResponse);
        
        queryResponse.then()
            .body("code", equalTo(200))
            .body("data.name", equalTo("test_business_metric"))
            .body("data.value", equalTo(123.45));
    }
    
    @Test
    @Order(21)
    @Story("自定义指标监控")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("METRIC-CUST-002: 指标查询时间范围测试")
    void testMetricQueryTimeRange() {
        // 查询最近1小时的CPU使用率
        Response response = get("/monitor/metrics/cpu?period=1h");
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.dataPoints", notNullValue())
            .body("data.dataPoints.size()", greaterThan(0))
            .body("data.dataPoints[0].timestamp", notNullValue())
            .body("data.dataPoints[0].value", notNullValue());
        
        log.info("获取到 {} 个数据点", response.jsonPath().getList("data.dataPoints").size());
    }
    
    @Test
    @Order(22)
    @Story("自定义指标监控")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("METRIC-CUST-003: 指标聚合测试")
    void testMetricAggregation() {
        // 查询最近24小时的指标聚合数据
        Response response = get("/monitor/metrics/cpu?period=24h&aggregation=avg");
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.avg", notNullValue())
            .body("data.min", notNullValue())
            .body("data.max", notNullValue())
            .body("data.stdev", notNullValue());
    }
}
