package com.qizhilian.api.monitor;

import com.qizhilian.api.base.ApiBaseTest;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * 监控告警测试基类
 * 提供监控告警API测试的基础配置和通用方法
 */
@Slf4j
public abstract class MonitorBaseTest extends ApiBaseTest {
    
    // ==================== 监控指标采集测试 ====================
    
    /**
     * 获取系统监控指标
     */
    @Step("获取系统监控指标: {endpoint}")
    protected Response getSystemMetrics(String endpoint) {
        return get(endpoint);
    }
    
    /**
     * 获取CPU使用率
     */
    @Step("获取CPU使用率")
    protected double getCpuUsage() {
        Response response = getSystemMetrics("/monitor/metrics/cpu");
        return response.jsonPath().getDouble("data.usage");
    }
    
    /**
     * 获取内存使用率
     */
    @Step("获取内存使用率")
    protected double getMemoryUsage() {
        Response response = getSystemMetrics("/monitor/metrics/memory");
        return response.jsonPath().getDouble("data.usage");
    }
    
    /**
     * 获取磁盘使用率
     */
    @Step("获取磁盘使用率")
    protected double getDiskUsage() {
        Response response = getSystemMetrics("/monitor/metrics/disk");
        return response.jsonPath().getDouble("data.usage");
    }
    
    /**
     * 获取数据库连接池状态
     */
    @Step("获取数据库连接池状态")
    protected Response getDbPoolStatus() {
        return getSystemMetrics("/monitor/metrics/db-pool");
    }
    
    /**
     * 获取JVM状态
     */
    @Step("获取JVM状态")
    protected Response getJvmStatus() {
        return getSystemMetrics("/monitor/metrics/jvm");
    }
    
    // ==================== 告警规则测试 ====================
    
    /**
     * 创建告警规则
     */
    @Step("创建告警规则")
    protected Response createAlertRule(Object rule) {
        return post("/alert/rules", rule);
    }
    
    /**
     * 更新告警规则
     */
    @Step("更新告警规则: {ruleId}")
    protected Response updateAlertRule(String ruleId, Object rule) {
        return put("/alert/rules/" + ruleId, rule);
    }
    
    /**
     * 删除告警规则
     */
    @Step("删除告警规则: {ruleId}")
    protected Response deleteAlertRule(String ruleId) {
        return delete("/alert/rules/" + ruleId);
    }
    
    /**
     * 获取告警规则列表
     */
    @Step("获取告警规则列表")
    protected Response getAlertRules() {
        return get("/alert/rules");
    }
    
    /**
     * 获取单个告警规则
     */
    @Step("获取告警规则: {ruleId}")
    protected Response getAlertRule(String ruleId) {
        return get("/alert/rules/" + ruleId);
    }
    
    /**
     * 测试告警规则触发
     */
    @Step("触发告警规则测试: {ruleId}")
    protected Response triggerAlertRuleTest(String ruleId) {
        return post("/alert/rules/" + ruleId + "/test", null);
    }
    
    // ==================== 告警通知测试 ====================
    
    /**
     * 发送测试告警通知
     */
    @Step("发送测试告警通知")
    protected Response sendTestAlertNotification(Object notification) {
        return post("/alert/notifications/test", notification);
    }
    
    /**
     * 获取告警通知历史
     */
    @Step("获取告警通知历史: {alertId}")
    protected Response getAlertNotificationHistory(String alertId) {
        return get("/alert/notifications/" + alertId + "/history");
    }
    
    /**
     * 获取通知channels列表
     */
    @Step("获取通知channels列表")
    protected Response getNotificationChannels() {
        return get("/alert/notifications/channels");
    }
    
    /**
     * 创建通知channel
     */
    @Step("创建通知channel")
    protected Response createNotificationChannel(Object channel) {
        return post("/alert/notifications/channels", channel);
    }
    
    /**
     * 删除通知channel
     */
    @Step("删除通知channel: {channelId}")
    protected Response deleteNotificationChannel(String channelId) {
        return delete("/alert/notifications/channels/" + channelId);
    }
    
    // ==================== 告警抑制规则测试 ====================
    
    /**
     * 创建告警抑制规则
     */
    @Step("创建告警抑制规则")
    protected Response createSuppressionRule(Object suppression) {
        return post("/alert/suppressions", suppression);
    }
    
    /**
     * 获取告警抑制规则列表
     */
    @Step("获取告警抑制规则列表")
    protected Response getSuppressionRules() {
        return get("/alert/suppressions");
    }
    
    /**
     * 删除告警抑制规则
     */
    @Step("删除告警抑制规则: {ruleId}")
    protected Response deleteSuppressionRule(String ruleId) {
        return delete("/alert/suppressions/" + ruleId);
    }
    
    // ==================== 告警管理测试 ====================
    
    /**
     * 获取告警列表
     */
    @Step("获取告警列表")
    protected Response getAlerts() {
        return get("/alert/alerts");
    }
    
    /**
     * 获取告警详情
     */
    @Step("获取告警详情: {alertId}")
    protected Response getAlertDetail(String alertId) {
        return get("/alert/alerts/" + alertId);
    }
    
    /**
     * 确认告警
     */
    @Step("确认告警: {alertId}")
    protected Response acknowledgeAlert(String alertId) {
        return post("/alert/alerts/" + alertId + "/acknowledge", null);
    }
    
    /**
     * 关闭告警
     */
    @Step("关闭告警: {alertId}")
    protected Response closeAlert(String alertId) {
        return post("/alert/alerts/" + alertId + "/close", null);
    }
    
    /**
     * 获取告警统计
     */
    @Step("获取告警统计")
    protected Response getAlertStats() {
        return get("/alert/statistics");
    }
}
