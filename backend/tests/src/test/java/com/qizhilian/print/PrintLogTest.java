package com.qizhilian.print;

import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Epic("打印模块")
@Feature("打印日志测试")
public class PrintLogTest {

    private static final String BASE_URL = System.getProperty("api.base.url", "http://localhost:8080");
    private static final String AUTH_TOKEN = System.getProperty("api.auth.token", "test-token");

    @BeforeAll
    void setUp() {
        RestAssured.baseURI = BASE_URL;
    }

    @Test
    @Order(1)
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
    @Order(2)
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
    @Order(3)
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
