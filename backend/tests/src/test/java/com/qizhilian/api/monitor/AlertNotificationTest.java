package com.qizhilian.api.monitor;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;

/**
 * 告警通知渠道测试
 * 验证告警通知能够通过各种渠道正确发送
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Epic("监控告警系统")
@Feature("告警通知渠道")
@DisplayName("告警通知渠道测试套件")
public class AlertNotificationTest extends MonitorBaseTest {
    
    // ==================== 通知Channel管理测试 ====================
    
    @Test
    @Order(1)
    @Story("通知渠道管理")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("NOTIFY-CHAN-001: 创建钉钉通知Channel")
    void testCreateDingtalkChannel() {
        Map<String, Object> channel = new HashMap<>();
        channel.put("name", "测试-钉钉-webhook");
        channel.put("type", "dingtalk");
        channel.put("config", Map.of(
            "webhook", "https://oapi.dingtalk.com/robot/send?access_token=test_token_123456",
            "secret", "SEC00000000000000000000000000000000"
        ));
        channel.put("enabled", true);
        channel.put("description", "用于测试的钉钉通知Channel");
        
        Response response = createNotificationChannel(channel);
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.id", notNullValue())
            .body("data.name", equalTo("测试-钉钉-webhook"))
            .body("data.type", equalTo("dingtalk"))
            .body("data.enabled", equalTo(true));
        
        log.info("钉钉通知Channel创建成功: {}", response.jsonPath().getString("data.id"));
    }
    
    @Test
    @Order(2)
    @Story("通知渠道管理")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("NOTIFY-CHAN-002: 创建企业微信通知Channel")
    void testCreateWechatChannel() {
        Map<String, Object> channel = new HashMap<>();
        channel.put("name", "测试-企业微信-webhook");
        channel.put("type", "wechat");
        channel.put("config", Map.of(
            "webhook", "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test_key_123456",
            "secret", ""
        ));
        channel.put("enabled", true);
        
        Response response = createNotificationChannel(channel);
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.type", equalTo("wechat"));
    }
    
    @Test
    @Order(3)
    @Story("通知渠道管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("NOTIFY-CHAN-003: 创建邮箱通知Channel")
    void testCreateEmailChannel() {
        Map<String, Object> channel = new HashMap<>();
        channel.put("name", "测试-邮箱通知");
        channel.put("type", "email");
        channel.put("config", Map.of(
            "smtpServer", "smtp.example.com",
            "smtpPort", 587,
            "fromAddress", "alerts@example.com",
            "username", "alerts@example.com",
            "password", "test_password"
        ));
        channel.put("enabled", true);
        
        Response response = createNotificationChannel(channel);
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.type", equalTo("email"));
    }
    
    @Test
    @Order(4)
    @Story("通知渠道管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("NOTIFY-CHAN-004: 创建短信通知Channel")
    void testCreateSmsChannel() {
        Map<String, Object> channel = new HashMap<>();
        channel.put("name", "测试-短信通知");
        channel.put("type", "sms");
        channel.put("config", Map.of(
            "provider", "aliyun",
            "accessKeyId", "test_access_key",
            "accessKeySecret", "test_secret",
            "signature", "阿里云短信签名"
        ));
        channel.put("enabled", true);
        
        Response response = createNotificationChannel(channel);
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.type", equalTo("sms"));
    }
    
    // ==================== 测试通知发送 ====================
    
    @Test
    @Order(10)
    @Story("通知测试")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("NOTIFY-TEST-001: 发送钉钉测试通知")
    void testSendDingtalkTestNotification() {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "dingtalk");
        notification.put("title", "测试告警通知");
        notification.put("message", "这是一条测试告警通知，用于验证钉钉通知渠道是否正常。");
        notification.put("Detail", Map.of(
            "环境", "测试环境",
            "时间", "2026-04-28 04:00:00",
            "告警级别", "warning"
        ));
        
        Response response = sendTestAlertNotification(notification);
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.status", equalTo("success"))
            .body("data.messageId", notNullValue());
        
        log.info("钉钉测试通知发送成功: {}", response.jsonPath().getString("data.messageId"));
    }
    
    @Test
    @Order(11)
    @Story("通知测试")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("NOTIFY-TEST-002: 发送企业微信测试通知")
    void testSendWechatTestNotification() {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "wechat");
        notification.put("title", "测试告警通知");
        notification.put("message", "这是一条测试告警通知，用于验证企业微信通知渠道是否正常。");
        
        Response response = sendTestAlertNotification(notification);
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.status", equalTo("success"));
    }
    
    @Test
    @Order(12)
    @Story("通知测试")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("NOTIFY-TEST-003: 发送邮件测试通知")
    void testSendEmailTestNotification() {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "email");
        notification.put("to", "test@example.com");
        notification.put("title", "测试告警通知 - 邮件");
        notification.put("message", "这是一条测试告警通知，用于验证邮件通知渠道是否正常。<br><br>环境：测试环境<br>时间：2026-04-28 04:00:00");
        
        Response response = sendTestAlertNotification(notification);
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.status", equalTo("success"));
    }
    
    // ==================== 告警通知历史查询 ====================
    
    @Test
    @Order(20)
    @Story("通知历史查询")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("NOTIFY-HIST-001: 查询告警通知历史")
    void testGetAlertNotificationHistory() {
        // 首先创建一个测试告警
        Map<String, Object> rule = new HashMap<>();
        rule.put("name", "测试告警历史查询规则");
        rule.put("description", "用于测试通知历史查询");
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
        
        String ruleId = createResponse.jsonPath().getString("data.id");
        
        // 查询通知历史
        Response historyResponse = get("/alert/notifications?ruleId=" + ruleId);
        
        assertSuccess(historyResponse);
        historyResponse.then()
            .body("code", equalTo(200))
            .body("data.records", notNullValue());
        
        log.info("查询到 {} 条通知记录", historyResponse.jsonPath().getInt("data.total"));
        
        // 清理
        deleteAlertRule(ruleId);
    }
    
    // ==================== 通知模板测试 ====================
    
    @Test
    @Order(30)
    @Story("通知模板管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("NOTIFY-TMP-001: 查询默认通知模板")
    void testGetDefaultNotificationTemplate() {
        Response response = get("/alert/notifications/templates/default");
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data.id", notNullValue())
            .body("data.name", notNullValue())
            .body("data.content", notNullValue());
        
        log.info("默认通知模板: {}", response.jsonPath().getString("data.name"));
    }
    
    @Test
    @Order(31)
    @Story("通知模板管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("NOTIFY-TMP-002: 查询通知模板列表")
    void testGetNotificationTemplates() {
        Response response = get("/alert/notifications/templates");
        
        assertSuccess(response);
        response.then()
            .body("code", equalTo(200))
            .body("data", notNullValue())
            .body("data.size()", greaterThan(0));
    }
    
    // ==================== 通知渠道状态测试 ====================
    
    @Test
    @Order(40)
    @Story("通知渠道状态")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("NOTIFY-STAT-001: 检查通知Channel状态")
    void testCheckNotificationChannelStatus() {
        // 创建测试channel
        Map<String, Object> channel = new HashMap<>();
        channel.put("name", "状态检查测试");
        channel.put("type", "dingtalk");
        channel.put("config", Map.of(
            "webhook", "https://oapi.dingtalk.com/robot/send?access_token=test_status_check"
        ));
        channel.put("enabled", true);
        
        Response createResponse = createNotificationChannel(channel);
        assertSuccess(createResponse);
        
        String channelId = createResponse.jsonPath().getString("data.id");
        
        // 检查状态
        Response statusResponse = get("/alert/notifications/channels/" + channelId + "/status");
        
        assertSuccess(statusResponse);
        statusResponse.then()
            .body("code", equalTo(200))
            .body("data.enabled", equalTo(true))
            .body("data.status", anyOf(equalTo("active"), equalTo("inactive"), equalTo("error")));
        
        log.info("Channel状态: {}", statusResponse.jsonPath().getString("data.status"));
        
        // 清理
        deleteNotificationChannel(channelId);
    }
    
    @Test
    @Order(41)
    @Story("通知渠道状态")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("NOTIFY-STAT-002: 测试通知Channel連通性")
    void testNotificationChannelConnectivity() {
        // 创建测试channel
        Map<String, Object> channel = new HashMap<>();
        channel.put("name", "連通性测试");
        channel.put("type", "wechat");
        channel.put("config", Map.of(
            "webhook", "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test_conn_check"
        ));
        channel.put("enabled", true);
        
        Response createResponse = createNotificationChannel(channel);
        assertSuccess(createResponse);
        
        String channelId = createResponse.jsonPath().getString("data.id");
        
        // 测试連通性
        Response connectivityResponse = post("/alert/notifications/channels/" + channelId + "/connectivity-test", null);
        
        // 连接测试可能失败（因为使用的是测试webhook），但应该返回明确的结果
        connectivityResponse.then()
            .statusCode(200);
        
        String status = connectivityResponse.jsonPath().getString("data.status");
        log.info("Connectivity test status: {}", status);
        
        // 清理
        deleteNotificationChannel(channelId);
    }
}
