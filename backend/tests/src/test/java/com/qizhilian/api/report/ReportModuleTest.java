package com.qizhilian.api.report;

import com.qizhilian.api.base.ApiBaseTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 报表模块API测试
 * 
 * 测试覆盖：
 * - 财务报表测试（资产负债表/利润表/现金流量表）
 * - 业务报表测试（销售/采购/库存报表）
 * - 报表通用功能测试（查询/导出/权限）
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReportModuleTest extends ApiBaseTest {

    private static final String BASE_PATH = "/api/v1/reports";

    // 财务报表测试
    @Test
    @Order(1)
    @DisplayName("资产负债表查询测试")
    void testGetBalanceSheet() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("period", "2026-03")
                .when()
                .get(BASE_PATH + "/balance-sheet")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data", notNullValue())
                .body("data.period", equalTo("2026-03"))
                .body("data.assets", greaterThan(0))
                .body("data.liabilities", greaterThan(0))
                .body("data.equity", greaterThan(0))
                .body("data.assets", equalTo_sumOf("data.liabilities", "data.equity"))
                .extract().response();

        String reportId = response.jsonPath().getString("data.reportId");
        Assertions.assertNotNull(reportId, "资产负债表ID不应为空");
    }

    @Test
    @Order(2)
    @DisplayName("资产负债表对比分析测试")
    void testCompareBalanceSheet() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("period1", "2026-02")
                .param("period2", "2026-03")
                .when()
                .get(BASE_PATH + "/balance-sheet/compare")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.period1", equalTo("2026-02"))
                .body("data.period2", equalTo("2026-03"))
                .body("data.comparisons", notNullValue())
                .body("data.comparisons.size()", greaterThan(0))
                .extract().response();
    }

    @Test
    @Order(3)
    @DisplayName("利润表查询测试")
    void testGetProfitStatement() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("period", "2026-03")
                .when()
                .get(BASE_PATH + "/profit-statement")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.period", equalTo("2026-03"))
                .body("data.revenue", greaterThan(0))
                .body("data.cost", greaterThan(0))
                .body("data.grossProfit", greaterThan(0))
                .body("data.netProfit", greaterThan(0))
                .extract().response();
    }

    @Test
    @Order(4)
    @DisplayName("利润表趋势分析测试")
    void testProfitStatementTrend() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("startDate", "2026-01")
                .param("endDate", "2026-03")
                .param("metric", "netProfit")
                .when()
                .get(BASE_PATH + "/profit-trend")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.trend", notNullValue())
                .body("data.trend.size()", equalTo(3))
                .extract().response();
    }

    @Test
    @Order(5)
    @DisplayName("现金流量表查询测试")
    void testGetCashFlowStatement() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("period", "2026-03")
                .when()
                .get(BASE_PATH + "/cash-flow")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.period", equalTo("2026-03"))
                .body("data.operatingCashFlow", notNullValue())
                .body("data.investingCashFlow", notNullValue())
                .body("data.financingCashFlow", notNullValue())
                .extract().response();
    }

    // 业务报表测试
    @Test
    @Order(6)
    @DisplayName("销售趋势分析测试")
    void testSalesTrend() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("startDate", "2026-01")
                .param("endDate", "2026-03")
                .param("dimension", "date")
                .when()
                .get(BASE_PATH + "/sales/trend")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.trend", notNullValue())
                .extract().response();
    }

    @Test
    @Order(7)
    @DisplayName("销售排行分析测试")
    void testSalesRanking() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("startDate", "2026-01")
                .param("endDate", "2026-03")
                .param("rankType", "product")
                .param("topN", 10)
                .when()
                .get(BASE_PATH + "/sales/ranking")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.rankings", notNullValue())
                .body("data.rankings.size()", greaterThan(0))
                .extract().response();
    }

    @Test
    @Order(8)
    @DisplayName("采购趋势分析测试")
    void testPurchaseTrend() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("startDate", "2026-01")
                .param("endDate", "2026-03")
                .when()
                .get(BASE_PATH + "/purchase/trend")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.trend", notNullValue())
                .extract().response();
    }

    @Test
    @Order(9)
    @DisplayName("供应商采购分析测试")
    void testSupplierPurchase() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("startDate", "2026-01")
                .param("endDate", "2026-03")
                .when()
                .get(BASE_PATH + "/purchase/supplier-analysis")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.suppliers", notNullValue())
                .extract().response();
    }

    @Test
    @Order(10)
    @DisplayName("库存周转分析测试")
    void testInventoryTurnover() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("startDate", "2026-01")
                .param("endDate", "2026-03")
                .when()
                .get(BASE_PATH + "/inventory/turnover")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.turnoverRate", greaterThan(0))
                .body("data.turnoverDays", greaterThan(0))
                .extract().response();
    }

    @Test
    @Order(11)
    @DisplayName("库存预警查询测试")
    void testInventoryWarning() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/inventory/warnings")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.warnings", notNullValue())
                .extract().response();
    }

    // 报表通用功能测试
    @Test
    @Order(12)
    @DisplayName("报表条件筛选测试")
    void testReportFilter() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("reportType", "sales")
                .param("filterConditions", "{\"region\":\"华东\",\"date\":{\"start\":\"2026-01\",\"end\":\"2026-03\"}}")
                .when()
                .get(BASE_PATH + "/filter")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.filtered", notNullValue())
                .extract().response();
    }

    @Test
    @Order(13)
    @DisplayName("报表导出Excel测试")
    void testExportExcel() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("reportType", "balance-sheet")
                .param("period", "2026-03")
                .param("format", "excel")
                .when()
                .get(BASE_PATH + "/export")
                .then()
                .statusCode(HttpStatus.OK.value())
                .header("Content-Type", containsString("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .extract().response();

        byte[] fileContent = response.asByteArray();
        Assertions.assertTrue(fileContent.length > 0, "导出的Excel文件不应为空");
    }

    @Test
    @Order(14)
    @DisplayName("报表导出PDF测试")
    void testExportPdf() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("reportType", "profit-statement")
                .param("period", "2026-03")
                .param("format", "pdf")
                .when()
                .get(BASE_PATH + "/export")
                .then()
                .statusCode(HttpStatus.OK.value())
                .header("Content-Type", containsString("application/pdf"))
                .extract().response();

        byte[] fileContent = response.asByteArray();
        Assertions.assertTrue(fileContent.length > 0, "导出的PDF文件不应为空");
    }

    @Test
    @Order(15)
    @DisplayName("报表数据权限测试")
    void testDataPermission() {
        // 测试普通财务人员权限
        Response response = given()
                .header("Authorization", "Bearer " + normalUserToken)
                .param("reportType", "balance-sheet")
                .param("period", "2026-03")
                .when()
                .get(BASE_PATH + "/balance-sheet")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();

        // 测试超级管理员权限
        Response adminResponse = given()
                .header("Authorization", "Bearer " + adminUserToken)
                .param("reportType", "balance-sheet")
                .param("period", "2026-03")
                .when()
                .get(BASE_PATH + "/balance-sheet")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.departmentData", nullValue())
                .body("data.allDepartmentsData", notNullValue())
                .extract().response();
    }

    // 错误场景测试
    @Test
    @Order(16)
    @DisplayName("未授权访问测试")
    void testUnauthorizedAccess() {
        given()
                .param("period", "2026-03")
                .when()
                .get(BASE_PATH + "/balance-sheet")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @Order(17)
    @DisplayName("无效期间参数测试")
    void testInvalidPeriod() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .param("period", "invalid")
                .when()
                .get(BASE_PATH + "/balance-sheet")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo(400));
    }

    @Test
    @Order(18)
    @DisplayName("报表不存在测试")
    void testReportNotFound() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .param("period", "2026-03")
                .when()
                .get(BASE_PATH + "/non-existent-report")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo(404));
    }
}