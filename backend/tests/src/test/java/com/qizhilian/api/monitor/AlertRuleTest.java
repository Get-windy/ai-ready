package com.qizhilian.api.monitor;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;

/**
 * 告警规则触发测试
 * 验证告警规则能够正确触发并记录告警事件
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Epic("监控告警系统")
@Feature("告警规则触发")
@DisplayName("告警规则触发测试套件")
public class AlertRuleTest extends MonitorBaseTest {
    
    private static String testRuleId;
    private static String testChannelId;
    
    @BeforeAll
    void setupTestData() {
        // 创建测试通知channel
        Map<String, Object> channel = createTestChannel();
        if (channel != null) {
            testChannelId = channel.get("id").toString();
        }
    }
    
    @AfterAll
    void cleanupTestData() {
        // 清理测试数据
        if (testRuleId != null) {
            deleteAlertRule(testRuleId);
        }
        if (testChannelId != null) {
            deleteNotificationChannel(testChannelId);
        }
    }
    
    /**
     * 创建测试用的通知channel
     */
    private Map<String, Object> createTestChannel() {
        Map<String, Object> channel = new HashMap<>();
        channel.put("name", "测试-钉钉-webhook");
        channel.put("type", "dingtalk");
        channel.put("config", Map.of(
            "webhook", "https://oapi.dingtalk.com/robot/send?access_token=test_token",
            "secret", "SEC00000000000000000000000000000000"
        ));
        channel.put("enabled", true);
        channel.put("description", "用于告警规则测试的钉钉通知channel");
        
        Response response = createNotificationChannel(channel);
        if (response.statusCode() == 200) {
            Map<String, Object> result = new HashMap<>();
            result.put("id", response.jsonPath().getString("data.id"));
            return result;
        }
        return null;
    }
    
    // ==================== 告警规则创建测试 ====================
    
    @Test
    @Order(1)
    @Story("告警规则管理")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("ALERT-RULE-001: 创建CPU使用率告警规则")
    void testCreateCpuAlertRule() {
        Map<String, Object> rule = new HashMap<>();
        rule.put("name", "CPU使用率过高告警-测试");
        rule.put("description", "当CPU使用率超过80%时触发告警");
        rule.put("metric", "cpu.usage");
        rule.put("condition", Map.of(
            "type", "threshold",
            "operator", ">",
            "value", 80.0
        ));
        rule.put("duration", "5m");
        rule.put("channels", new String[]{testChannelId});
        rule.put("enabled", true);
        
        Response response = createAlertRule(rule);
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.id", notNullValue())
            .body("data.name", equalTo("CPU使用率过高告警-测试"))
            .body("data.enabled", equalTo(true));
        
        testRuleId = response.jsonPath().getString("data.id");
        log.info("创建CPU告警规则成功: {}", testRuleId);
    }
    
    @Test
    @Order(2)
    @Story("告警规则管理")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("ALERT-RULE-002: 创建内存使用率告警规则")
    void testCreateMemoryAlertRule() {
        Map<String, Object> rule = new HashMap<>();
        rule.put("name", "内存使用率过高告警-测试");
        rule.put("description", "当内存使用率超过85%时触发告警");
        rule.put("metric", "memory.usage");
        rule.put("condition", Map.of(
            "type", "threshold",
            "operator", ">",
            "value", 85.0
        ));
        rule.put("duration", "3m");
        rule.put("channels", new String[]{testChannelId});
        rule.put("enabled", true);
        
        Response response = createAlertRule(rule);
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.id", notNullValue())
            .body("data.metric", equalTo("memory.usage"));
    }
    
    @Test
    @Order(3)
    @Story("告警规则管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("ALERT-RULE-003: 创建数据库连接池告警规则")
    void testCreateDbPoolAlertRule() {
        Map<String, Object> rule = new HashMap<>();
        rule.put("name", "数据库连接池耗尽告警-测试");
        rule.put("description", "当数据库连接池活跃连接数超过90%时触发告警");
        rule.put("metric", "db.pool.active.ratio");
        rule.put("condition", Map.of(
            "type", "threshold",
            "operator", ">",
            "value", 0.9
        ));
        rule.put("duration", "1m");
        rule.put("channels", new String[]{testChannelId});
        rule.put("enabled", true);
        
        Response response = createAlertRule(rule);
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.metric", equalTo("db.pool.active.ratio"));
    }
    
    // ==================== 告警规则查询测试 ====================
    
    @Test
    @Order(10)
    @Story("告警规则管理")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("ALERT-RULE-010: 查询告警规则列表")
    void testGetAlertRules() {
        Response response = getAlertRules();
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data", notNullValue())
            .body("data.size()", greaterThan(0))
            .body("data.find { it.name == 'CPU使用率过高告警-测试' }.id", notNullValue());
    }
    
    @Test
    @Order(11)
    @Story("告警规则管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("ALERT-RULE-011: 查询单个告警规则详情")
    void testGetAlertRuleDetail() {
        if (testRuleId == null) {
            // 先创建测试规则
            testCreateCpuAlertRule();
        }
        
        Response response = getAlertRule(testRuleId);
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.id", equalTo(testRuleId))
            .body("data.name", equalTo("CPU使用率过高告警-测试"));
        
        log.info("告警规则详情: {}", response.prettyPrint());
    }
    
    // ==================== 告警规则更新测试 ====================
    
    @Test
    @Order(20)
    @Story("告警规则管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("ALERT-RULE-020: 更新告警规则阈值")
    void testUpdateAlertRule() {
        if (testRuleId == null) {
            testCreateCpuAlertRule();
        }
        
        Map<String, Object> update = new HashMap<>();
        update.put("name", "CPU使用率过高告警-已更新");
        update.put("condition", Map.of(
            "type", "threshold",
            "operator", ">",
            "value", 90.0
        ));
        
        Response response = updateAlertRule(testRuleId, update);
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.name", equalTo("CPU使用率过高告警-已更新"))
            .body("data.condition.value", equalTo(90.0));
    }
    
    // ==================== 告警规则测试触发 ====================
    
    @Test
    @Order(30)
    @Story("告警规则测试")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("ALERT-RULE-030: 触发告警规则测试")
    void testTriggerAlertRule() {
        if (testRuleId == null) {
            testCreateCpuAlertRule();
        }
        
        Response response = triggerAlertRuleTest(testRuleId);
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.triggered", notNullValue())
            .body("data deliberation", notNullValue());
        
        Boolean triggered = response.jsonPath().getBoolean("data.triggered");
        if (triggered != null && triggered) {
            log.info("告警规则测试触发成功");
        } else {
            log.info("告警规则测试未触发（指标值未达到阈值）");
        }
    }
    
    // ==================== 告警规则删除测试 ====================
    
    @Test
    @Order(40)
    @Story("告警规则管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("ALERT-RULE-040: 删除告警规则")
    void testDeleteAlertRule() {
        // 创建临时规则用于删除测试
        Map<String, Object> rule = new HashMap<>();
        rule.put("name", "临时删除测试规则");
        rule.put("description", "用于测试删除操作的规则");
        rule.put("metric", "cpu.usage");
        rule.put("condition", Map.of(
            "type", "threshold",
            "operator", ">",
            "value", 95.0
        ));
        rule.put("duration", "1m");
        rule.put("enabled", true);
        
        Response createResponse = createAlertRule(rule);
        assertSuccess(createResponse);
        
        String tempRuleId = createResponse.jsonPath().getString("data.id");
        log.info("创建临时规则用于删除测试: {}", tempRuleId);
        
        // 执行删除
        Response deleteResponse = deleteAlertRule(tempRuleId);
        
        assertSuccess(deleteResponse);
        response.then()
            .body("code", equalTo(200));
        
        // 验证删除后查询不到
        Response queryResponse = getAlertRule(tempRuleId);
        queryResponse.then()
            .statusCode(404);
        
        log.info("告警规则删除成功: {}", tempRuleId);
    }
    
    // ==================== 告警抑制规则测试 ====================
    
    @Test
    @Order(50)
    @Story("告警抑制规则")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("ALERT-SUPPR-001: 创建告警抑制规则")
    void testCreateSuppressionRule() {
        Map<String, Object> suppression = new HashMap<>();
        suppression.put("name", "维护窗口抑制规则-测试");
        suppression.put("description", "在维护窗口期间抑制非关键告警");
        suppression.put("timeRange", Map.of(
            "start", "02:00",
            "end", "06:00",
            "timezone", "Asia/Shanghai"
        ));
        suppression.put("rules", new String[]{testRuleId});
        suppression.put("enabled", true);
        
        Response response = createSuppressionRule(suppression);
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.id", notNullValue())
            .body("data.name", equalTo("维护窗口抑制规则-测试"));
    }
    
    @Test
    @Order(51)
    @Story("告警抑制规则")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("ALERT-SUPPR-002: 查询告警抑制规则")
    void testGetSuppressionRules() {
        Response response = getSuppressionRules();
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data", notNullValue())
            .body("data.size()", greaterThanOrEqualTo(0));
    }
    
    @Test
    @Order(52)
    @Story("告警抑制规则")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("ALERT-SUPPR-003: 删除告警抑制规则")
    void testDeleteSuppressionRule() {
        // 先创建一个抑制规则
        Map<String, Object> suppression = new HashMap<>();
        suppression.put("name", "临时抑制规则测试");
        suppression.put("description", "用于测试删除的抑制规则");
        suppression.put("timeRange", Map.of(
            "start", "03:00",
            "end", "04:00"
        ));
        suppression.put("rules", new String[]{});
        suppression.put("enabled", true);
        
        Response createResponse = createSuppressionRule(suppression);
        assertSuccess(createResponse);
        
        String suppressionId = createResponse.jsonPath().getString("data.id");
        log.info("创建临时抑制规则: {}", suppressionId);
        
        // 删除
        Response deleteResponse = deleteSuppressionRule(suppressionId);
        assertSuccess(deleteResponse);
        
        log.info("抑制规则删除成功: {}", suppressionId);
    }
}
