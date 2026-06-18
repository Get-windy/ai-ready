package com.qizhilian.audit;

import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Epic("日志审计模块")
@Feature("日志审计功能测试")
public class LogAuditTest {

    private static final String BASE_URL = System.getProperty("api.base.url", "http://localhost:8080");
    private static final String AUTH_TOKEN = System.getProperty("api.auth.token", "test-token");

    @BeforeAll
    void setUp() {
        RestAssured.baseURI = BASE_URL;
    }

    // ==================== 操作日志记录测试 ====================

    @Test
    @Order(1)
    @Story("操作日志记录")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AUDIT-LOG-001: 用户操作日志记录测试")
    void testUserOperationLog() {
        Map<String, Object> log = new HashMap<>();
        log.put("userId", "user001");
        log.put("operation", "CREATE");
        log.put("module", "客户管理");
        log.put("targetId", "cust001");
        log.put("details", "创建新客户");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(log)
        .when()
            .post("/api/v1/audit/logs")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getString("data.id")).isNotNull();
    }

    @Test
    @Order(2)
    @Story("操作日志记录")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AUDIT-LOG-002: 系统操作日志记录测试")
    void testSystemOperationLog() {
        Map<String, Object> log = new HashMap<>();
        log.put("operation", "SCHEDULED_TASK");
        log.put("module", "定时任务");
        log.put("taskName", "数据同步任务");
        log.put("result", "SUCCESS");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(log)
        .when()
            .post("/api/v1/audit/logs/system")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(3)
    @Story("操作日志记录")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AUDIT-LOG-003: 敏感操作日志记录测试")
    void testSensitiveOperationLog() {
        Map<String, Object> log = new HashMap<>();
        log.put("userId", "admin001");
        log.put("operation", "PERMISSION_CHANGE");
        log.put("module", "权限管理");
        log.put("targetId", "role001");
        log.put("isSensitive", true);

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(log)
        .when()
            .post("/api/v1/audit/logs/sensitive")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getBoolean("data.isSensitive")).isTrue();
    }

    // ==================== 日志查询测试 ====================

    @Test
    @Order(4)
    @Story("日志查询")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AUDIT-QUERY-001: 按用户查询日志测试")
    void testQueryLogByUser() {
        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .queryParam("userId", "user001")
            .queryParam("page", 1)
            .queryParam("size", 20)
        .when()
            .get("/api/v1/audit/logs")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getList("data.records")).isNotNull();
    }

    @Test
    @Order(5)
    @Story("日志查询")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AUDIT-QUERY-002: 按时间范围查询日志测试")
    void testQueryLogByTimeRange() {
        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .queryParam("startTime", "2024-01-01T00:00:00")
            .queryParam("endTime", "2024-12-31T23:59:59")
            .queryParam("page", 1)
            .queryParam("size", 20)
        .when()
            .get("/api/v1/audit/logs")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(6)
    @Story("日志查询")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("AUDIT-QUERY-003: 按操作类型查询日志测试")
    void testQueryLogByOperationType() {
        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .queryParam("operationType", "CREATE")
            .queryParam("page", 1)
            .queryParam("size", 20)
        .when()
            .get("/api/v1/audit/logs")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(7)
    @Story("日志查询")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("AUDIT-QUERY-004: 日志导出功能测试")
    void testExportLogs() {
        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .queryParam("startTime", "2024-01-01T00:00:00")
            .queryParam("endTime", "2024-12-31T23:59:59")
            .queryParam("format", "excel")
        .when()
            .get("/api/v1/audit/logs/export")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.getHeader("Content-Type")).contains("application/vnd.openxmlformats");
    }

    // ==================== 日志安全测试 ====================

    @Test
    @Order(8)
    @Story("日志安全")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AUDIT-SEC-001: 日志防篡改测试")
    void testLogTamperProof() {
        String logId = "log001";
        
        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
        .when()
            .get("/api/v1/audit/logs/" + logId + "/hash")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getString("data.hash")).isNotNull();
    }

    @Test
    @Order(9)
    @Story("日志安全")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AUDIT-SEC-002: 日志访问权限控制测试")
    void testLogAccessControl() {
        Response response = given()
            .header("Authorization", "Bearer " + "invalid-token")
        .when()
            .get("/api/v1/audit/logs")
        .then()
            .statusCode(401)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("401");
    }

    @Test
    @Order(10)
    @Story("日志安全")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("AUDIT-SEC-003: 日志加密存储测试")
    void testLogEncryptedStorage() {
        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
        .when()
            .get("/api/v1/audit/config/encryption")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getBoolean("data.enabled")).isTrue();
    }

    @Test
    @Order(11)
    @Story("日志安全")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("AUDIT-SEC-004: 日志备份和归档测试")
    void testLogBackup() {
        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
        .when()
            .post("/api/v1/audit/logs/backup")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    // ==================== 审计报表测试 ====================

    @Test
    @Order(12)
    @Story("审计报表")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("AUDIT-RPT-001: 操作统计报表测试")
    void testOperationStatistics() {
        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .queryParam("startDate", "2024-01-01")
            .queryParam("endDate", "2024-12-31")
        .when()
            .get("/api/v1/audit/reports/statistics")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getMap("data")).isNotNull();
    }

    @Test
    @Order(13)
    @Story("审计报表")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("AUDIT-RPT-002: 异常操作分析测试")
    void testAnomalyAnalysis() {
        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .queryParam("startDate", "2024-01-01")
            .queryParam("endDate", "2024-12-31")
        .when()
            .get("/api/v1/audit/reports/anomaly")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(14)
    @Story("审计报表")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("AUDIT-RPT-003: 合规性审计报告测试")
    void testComplianceReport() {
        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .queryParam("type", "SOX")
            .queryParam("startDate", "2024-01-01")
            .queryParam("endDate", "2024-12-31")
        .when()
            .get("/api/v1/audit/reports/compliance")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }
}