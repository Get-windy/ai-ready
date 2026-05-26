package com.qizhilian.print;

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
@Epic("打印模块")
@Feature("打印功能测试")
public class PrintModuleTest {

    private static final String BASE_URL = System.getProperty("api.base.url", "http://localhost:8080");
    private static final String AUTH_TOKEN = System.getProperty("api.auth.token", "test-token");
    
    private String templateId;
    private String printerId;
    private String taskId;

    @BeforeAll
    void setUp() {
        RestAssured.baseURI = BASE_URL;
    }

    // ==================== 打印模板测试 ====================

    @Test
    @Order(1)
    @Story("模板管理")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("PRINT-TPL-001: 模板创建测试")
    void testCreateTemplate() {
        Map<String, Object> template = new HashMap<>();
        template.put("name", "销售订单打印模板");
        template.put("type", "SALES_ORDER");
        template.put("content", "{\"header\":\"销售订单\",\"fields\":[\"orderNo\",\"customer\",\"amount\"]}");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(template)
        .when()
            .post("/api/v1/print/templates")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        templateId = response.jsonPath().getString("data.id");
        assertThat(templateId).isNotNull();
    }

    @Test
    @Order(2)
    @Story("模板管理")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("PRINT-TPL-002: 模板编辑测试")
    void testEditTemplate() {
        Map<String, Object> update = new HashMap<>();
        update.put("name", "销售订单打印模板-更新");
        update.put("content", "{\"header\":\"销售订单\",\"fields\":[\"orderNo\",\"customer\",\"amount\",\"remark\"]}");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(update)
        .when()
            .put("/api/v1/print/templates/" + templateId)
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(3)
    @Story("模板管理")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("PRINT-TPL-003: 模板预览测试")
    void testPreviewTemplate() {
        Map<String, Object> previewData = new HashMap<>();
        previewData.put("orderNo", "SO2024001");
        previewData.put("customer", "测试客户");
        previewData.put("amount", 10000.00);

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(previewData)
        .when()
            .post("/api/v1/print/templates/" + templateId + "/preview")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(4)
    @Story("模板管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("PRINT-TPL-004: 模板复制测试")
    void testCopyTemplate() {
        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .queryParam("newName", "销售订单模板副本")
        .when()
            .post("/api/v1/print/templates/" + templateId + "/copy")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(5)
    @Story("模板管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("PRINT-TPL-005: 模板删除测试")
    void testDeleteTemplate() {
        Map<String, Object> template = new HashMap<>();
        template.put("name", "临时删除模板");
        template.put("type", "TEMP");
        template.put("content", "{}");

        Response createResp = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(template)
        .when()
            .post("/api/v1/print/templates")
        .then()
            .statusCode(200)
            .extract().response();

        String tempId = createResp.jsonPath().getString("data.id");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
        .when()
            .delete("/api/v1/print/templates/" + tempId)
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    // ==================== 打印任务测试 ====================

    @Test
    @Order(10)
    @Story("打印任务")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("PRINT-TASK-001: 打印任务创建测试")
    void testCreatePrintTask() {
        Map<String, Object> printer = new HashMap<>();
        printer.put("name", "测试打印机");
        printer.put("ipAddress", "192.168.1.100");
        printer.put("port", 9100);
        printer.put("type", "THERMAL");

        Response printerResp = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(printer)
        .when()
            .post("/api/v1/print/printers")
        .then()
            .statusCode(200)
            .extract().response();

        printerId = printerResp.jsonPath().getString("data.id");

        Map<String, Object> task = new HashMap<>();
        task.put("templateId", templateId);
        task.put("printerId", printerId);
        task.put("documentId", "DOC001");
        task.put("documentType", "SALES_ORDER");
        task.put("copies", 1);

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(task)
        .when()
            .post("/api/v1/print/tasks")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        taskId = response.jsonPath().getString("data.id");
        assertThat(response.jsonPath().getString("data.status")).isEqualTo("PENDING");
    }

    @Test
    @Order(11)
    @Story("打印任务")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("PRINT-TASK-002: 打印队列管理测试")
    void testPrintQueueManagement() {
        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
        .when()
            .get("/api/v1/print/tasks/queue")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        List<Map<String, Object>> queue = response.jsonPath().getList("data");
        assertThat(queue).isNotNull();
    }

    @Test
    @Order(12)
    @Story("打印任务")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("PRINT-TASK-003: 打印状态查询测试")
    void testPrintStatusQuery() {
        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
        .when()
            .get("/api/v1/print/tasks/" + taskId)
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getString("data.id")).isEqualTo(taskId);
        assertThat(response.jsonPath().getString("data.status")).isNotNull();
    }

    @Test
    @Order(13)
    @Story("打印任务")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("PRINT-TASK-004: 批量打印测试")
    void testBatchPrint() {
        Map<String, Object> batchTask = new HashMap<>();
        batchTask.put("templateId", templateId);
        batchTask.put("printerId", printerId);
        batchTask.put("documentIds", List.of("DOC001", "DOC002", "DOC003"));
        batchTask.put("documentType", "SALES_ORDER");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(batchTask)
        .when()
            .post("/api/v1/print/tasks/batch")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getList("data.taskIds")).hasSize(3);
    }

    @Test
    @Order(14)
    @Story("打印任务")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("PRINT-TASK-005: 打印任务取消测试")
    void testCancelPrintTask() {
        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
        .when()
            .post("/api/v1/print/tasks/" + taskId + "/cancel")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(15)
    @Story("打印任务")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("PRINT-TASK-006: 打印历史查询测试")
    void testPrintHistoryQuery() {
        Map<String, Object> params = new HashMap<>();
        params.put("startDate", "2024-01-01");
        params.put("endDate", "2024-12-31");
        params.put("documentType", "SALES_ORDER");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .queryParams(params)
        .when()
            .get("/api/v1/print/tasks/history")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    // ==================== 打印机管理测试 ====================

    @Test
    @Order(20)
    @Story("打印机管理")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("PRINT-PRTR-001: 打印机注册测试")
    void testRegisterPrinter() {
        Map<String, Object> printer = new HashMap<>();
        printer.put("name", "前台打印机");
        printer.put("ipAddress", "192.168.1.101");
        printer.put("port", 9100);
        printer.put("type", "LASER");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(printer)
        .when()
            .post("/api/v1/print/printers")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getString("data.status")).isEqualTo("ONLINE");
    }

    @Test
    @Order(21)
    @Story("打印机管理")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("PRINT-PRTR-002: 打印机状态测试")
    void testPrinterStatus() {
        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
        .when()
            .get("/api/v1/print/printers/" + printerId + "/status")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getString("data.status")).isNotNull();
    }

    @Test
    @Order(22)
    @Story("打印机管理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("PRINT-PRTR-003: 打印机分组测试")
    void testPrinterGroup() {
        Map<String, Object> group = new HashMap<>();
        group.put("name", "销售部打印机");
        group.put("printerIds", List.of(printerId));
        group.put("isDefault", true);

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .body(group)
        .when()
            .post("/api/v1/print/printers/groups")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");

    // ==================== 打印日志测试 ====================

    @Test
    @Order(30)
    @Story("打印日志")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("PRINT-LOG-001: 打印记录查询测试")
    void testPrintLogQuery() {
        Map<String, Object> params = new HashMap<>();
        params.put("page", 1);
        params.put("size", 20);
        params.put("startDate", "2024-01-01");
        params.put("endDate", "2024-12-31");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .queryParams(params)
        .when()
            .get("/api/v1/print/logs")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getList("data.records")).isNotNull();
    }

    @Test
    @Order(31)
    @Story("打印日志")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("PRINT-LOG-002: 打印统计测试")
    void testPrintStatistics() {
        Map<String, Object> params = new HashMap<>();
        params.put("startDate", "2024-01-01");
        params.put("endDate", "2024-12-31");
        params.put("groupBy", "printer");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .queryParams(params)
        .when()
            .get("/api/v1/print/logs/statistics")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getMap("data")).isNotNull();
    }

    @Test
    @Order(32)
    @Story("打印日志")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("PRINT-LOG-003: 打印日志导出测试")
    void testPrintLogExport() {
        Map<String, Object> params = new HashMap<>();
        params.put("startDate", "2024-01-01");
        params.put("endDate", "2024-12-31");
        params.put("format", "excel");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .queryParams(params)
        .when()
            .get("/api/v1/print/logs/export")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.getHeader("Content-Type")).contains("application/vnd.openxmlformats");
    }
}

