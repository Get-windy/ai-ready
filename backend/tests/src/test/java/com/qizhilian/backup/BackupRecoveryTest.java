package com.qizhilian.backup;

import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Epic("数据备份恢复模块")
@Feature("数据备份恢复功能测试")
public class BackupRecoveryTest {

    private static final String BASE_URL = System.getProperty("api.base.url", "http://localhost:8080");
    private static final String AUTH_TOKEN = System.getProperty("api.auth.token", "test-token");

    @BeforeAll
    void setUp() {
        RestAssured.baseURI = BASE_URL;
    }

    // ==================== 全量备份测试 ====================

    @Test
    @Order(1)
    @Story("全量备份")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("BACKUP-FULL-001: 数据库全量备份测试")
    void testDatabaseFullBackup() {
        Map<String, Object> backupConfig = new HashMap<>();
        backupConfig.put("type", "DATABASE");
        backupConfig.put("targetPath", "/backup/db/full/");
        backupConfig.put("compress", true);
        backupConfig.put("encrypt", false);

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(backupConfig)
        .when()
            .post("/api/v1/backup/full")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getString("data.backupId")).isNotNull();
    }

    @Test
    @Order(2)
    @Story("全量备份")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("BACKUP-FULL-002: 文件系统全量备份测试")
    void testFileSystemFullBackup() {
        Map<String, Object> backupConfig = new HashMap<>();
        backupConfig.put("type", "FILE_SYSTEM");
        backupConfig.put("sourcePath", "/data/");
        backupConfig.put("targetPath", "/backup/files/full/");
        backupConfig.put("includeHidden", false);

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(backupConfig)
        .when()
            .post("/api/v1/backup/full")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(3)
    @Story("全量备份")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("BACKUP-FULL-003: 全量备份压缩测试")
    void testFullBackupCompression() {
        Map<String, Object> backupConfig = new HashMap<>();
        backupConfig.put("type", "DATABASE");
        backupConfig.put("targetPath", "/backup/db/compressed/");
        backupConfig.put("compress", true);
        backupConfig.put("compressionLevel", 9);

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(backupConfig)
        .when()
            .post("/api/v1/backup/full")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getLong("data.compressionRatio")).isGreaterThan(0L);
    }

    @Test
    @Order(4)
    @Story("全量备份")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("BACKUP-FULL-004: 全量备份加密测试")
    void testFullBackupEncryption() {
        Map<String, Object> backupConfig = new HashMap<>();
        backupConfig.put("type", "DATABASE");
        backupConfig.put("targetPath", "/backup/db/encrypted/");
        backupConfig.put("encrypt", true);
        backupConfig.put("encryptionAlgorithm", "AES-256");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(backupConfig)
        .when()
            .post("/api/v1/backup/full")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getBoolean("data.encrypted")).isTrue();
    }

    // ==================== 增量备份测试 ====================

    @Test
    @Order(5)
    @Story("增量备份")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("BACKUP-INC-001: 数据库增量备份测试")
    void testDatabaseIncrementalBackup() {
        Map<String, Object> backupConfig = new HashMap<>();
        backupConfig.put("type", "DATABASE");
        backupConfig.put("baseBackupId", "backup_001");
        backupConfig.put("targetPath", "/backup/db/incremental/");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(backupConfig)
        .when()
            .post("/api/v1/backup/incremental")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getString("data.backupId")).isNotNull();
    }

    @Test
    @Order(6)
    @Story("增量备份")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("BACKUP-INC-002: 文件系统增量备份测试")
    void testFileSystemIncrementalBackup() {
        Map<String, Object> backupConfig = new HashMap<>();
        backupConfig.put("type", "FILE_SYSTEM");
        backupConfig.put("baseBackupId", "backup_002");
        backupConfig.put("sourcePath", "/data/");
        backupConfig.put("targetPath", "/backup/files/incremental/");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(backupConfig)
        .when()
            .post("/api/v1/backup/incremental")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(7)
    @Story("增量备份")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("BACKUP-INC-003: 增量备份链完整性测试")
    void testIncrementalChainIntegrity() {
        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .queryParam("backupId", "backup_003")
        .when()
            .get("/api/v1/backup/chain/integrity")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getBoolean("data.integrity")).isTrue();
    }

    // ==================== 定时备份测试 ====================

    @Test
    @Order(8)
    @Story("定时备份")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("BACKUP-SCHED-001: 定时备份计划设置测试")
    void testScheduledBackupSetup() {
        Map<String, Object> schedule = new HashMap<>();
        schedule.put("name", "Daily Database Backup");
        schedule.put("type", "DATABASE");
        schedule.put("cronExpression", "0 0 2 * * ?");
        schedule.put("enabled", true);
        schedule.put("retentionDays", 30);

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(schedule)
        .when()
            .post("/api/v1/backup/schedule")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getString("data.scheduleId")).isNotNull();
    }

    @Test
    @Order(9)
    @Story("定时备份")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("BACKUP-SCHED-002: 定时备份执行测试")
    void testScheduledBackupExecution() {
        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .queryParam("scheduleId", "sched_001")
        .when()
            .post("/api/v1/backup/schedule/execute")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getString("data.executionId")).isNotNull();
    }

    @Test
    @Order(10)
    @Story("定时备份")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("BACKUP-SCHED-003: 备份通知功能测试")
    void testBackupNotification() {
        Map<String, Object> notification = new HashMap<>();
        notification.put("scheduleId", "sched_001");
        notification.put("notificationType", "EMAIL");
        notification.put("recipients", new String[]{"admin@example.com"});
        notification.put("onSuccess", true);
        notification.put("onFailure", true);

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(notification)
        .when()
            .post("/api/v1/backup/notification")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    // ==================== 数据恢复测试 ====================

    @Test
    @Order(11)
    @Story("数据恢复")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("RESTORE-001: 全量恢复测试")
    void testFullRestore() {
        Map<String, Object> restoreConfig = new HashMap<>();
        restoreConfig.put("backupId", "backup_001");
        restoreConfig.put("targetPath", "/restore/");
        restoreConfig.put("overwrite", true);

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(restoreConfig)
        .when()
            .post("/api/v1/restore/full")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getBoolean("data.success")).isTrue();
    }

    @Test
    @Order(12)
    @Story("数据恢复")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("RESTORE-002: 增量恢复测试")
    void testIncrementalRestore() {
        Map<String, Object> restoreConfig = new HashMap<>();
        restoreConfig.put("backupId", "backup_003");
        restoreConfig.put("targetPath", "/restore/");
        restoreConfig.put("applyAllIncrements", true);

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(restoreConfig)
        .when()
            .post("/api/v1/restore/incremental")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(13)
    @Story("数据恢复")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("RESTORE-003: 时间点恢复测试")
    void testPointInTimeRestore() {
        Map<String, Object> restoreConfig = new HashMap<>();
        restoreConfig.put("targetTime", "2024-04-15T10:00:00Z");
        restoreConfig.put("targetPath", "/restore/pit/");
        restoreConfig.put("databaseName", "erp_db");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(restoreConfig)
        .when()
            .post("/api/v1/restore/point-in-time")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(14)
    @Story("数据恢复")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("RESTORE-004: 部分数据恢复测试")
    void testPartialRestore() {
        Map<String, Object> restoreConfig = new HashMap<>();
        restoreConfig.put("backupId", "backup_001");
        restoreConfig.put("targetPath", "/restore/partial/");
        restoreConfig.put("tables", new String[]{"customers", "orders"});
        restoreConfig.put("excludeTables", new String[]{"audit_logs"});

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(restoreConfig)
        .when()
            .post("/api/v1/restore/partial")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }
}