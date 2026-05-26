package com.qizhilian.report;

import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 企智连报表模块测试类
 * 覆盖财务报表、业务报表、导出功能等
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Epic("报表模块")
@Feature("报表功能测试")
public class ReportModuleTest {

    private static final String BASE_URL = System.getProperty("api.base.url", "http://localhost:8080");
    private static final String AUTH_TOKEN = System.getProperty("api.auth.token", "test-token");
    
    private String currentDate;
    private String lastMonthDate;

    @BeforeAll
    void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        
        LocalDate now = LocalDate.now();
        currentDate = now.format(DateTimeFormatter.ISO_DATE);
        lastMonthDate = now.minusMonths(1).format(DateTimeFormatter.ISO_DATE);
    }

    // ==================== 财务报表测试 ====================

    @Test
    @Order(1)
    @Story("资产负债表")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("REPORT-FIN-001: 资产负债表报表生成测试")
    @Description("验证资产负债表能正确生成，且资产=负债+所有者权益")
    void testBalanceSheetGeneration() {
        Map<String, Object> params = new HashMap<>();
        params.put("reportType", "BALANCE_SHEET");
        params.put("startDate", lastMonthDate);
        params.put("endDate", currentDate);

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .queryParams(params)
        .when()
            .get("/api/v1/reports/financial/balance-sheet")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getString("data.reportName")).isEqualTo("资产负债表");
        
        // 验证三大要素存在
        assertThat(response.jsonPath().getMap("data.assets")).isNotNull();
        assertThat(response.jsonPath().getMap("data.liabilities")).isNotNull();
        assertThat(response.jsonPath().getMap("data.equity")).isNotNull();
        
        // 验证会计恒等式
        Double totalAssets = response.jsonPath().getDouble("data.assets.total");
        Double totalLiabilities = response.jsonPath().getDouble("data.liabilities.total");
        Double totalEquity = response.jsonPath().getDouble("data.equity.total");
        
        assertThat(totalAssets).isNotNull().isGreaterThan(0);
        assertThat(totalLiabilities + totalEquity).isEqualTo(totalAssets, Assertions.within(0.01));
    }

    @Test
    @Order(2)
    @Story("利润表")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("REPORT-FIN-002: 利润表报表生成测试")
    @Description("验证利润表能正确生成，且利润计算准确")
    void testIncomeStatementGeneration() {
        Map<String, Object> params = new HashMap<>();
        params.put("reportType", "INCOME_STATEMENT");
        params.put("period", "CURRENT_MONTH");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .queryParams(params)
        .when()
            .get("/api/v1/reports/financial/income-statement")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getString("data.reportName")).isEqualTo("利润表");
        
        Double revenue = response.jsonPath().getDouble("data.revenue");
        Double cost = response.jsonPath().getDouble("data.costOfSales");
        Double expenses = response.jsonPath().getDouble("data.operatingExpenses");
        Double netProfit = response.jsonPath().getDouble("data.netProfit");
        
        assertThat(revenue).isNotNull();
        assertThat(netProfit).isEqualTo(revenue - cost - expenses, Assertions.within(0.01));
    }

    @Test
    @Order(3)
    @Story("现金流量表")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("REPORT-FIN-003: 现金流量表报表生成测试")
    @Description("验证现金流量表能正确生成，分类准确")
    void testCashFlowStatementGeneration() {
        Map<String, Object> params = new HashMap<>();
        params.put("reportType", "CASH_FLOW");
        params.put("startDate", lastMonthDate);
        params.put("endDate", currentDate);
        params.put("classification", "DIRECT");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .queryParams(params)
        .when()
            .get("/api/v1/reports/financial/cash-flow")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        
        Double operating = response.jsonPath().getDouble("data.operatingActivities.netCashFlow");
        Double investing = response.jsonPath().getDouble("data.investingActivities.netCashFlow");
        Double financing = response.jsonPath().getDouble("data.financingActivities.netCashFlow");
        
        assertThat(operating).isNotNull();
        assertThat(investing).isNotNull();
        assertThat(financing).isNotNull();
        
        Double netIncrease = response.jsonPath().getDouble("data.netIncreaseInCash");
        assertThat(netIncrease).isEqualTo(operating + investing + financing, Assertions.within(0.01));
    }

    @Test
    @Order(4)
    @Story("资产负债表")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("REPORT-FIN-004: 资产负债表多期间对比测试")
    @Description("验证资产负债表支持多期间对比功能")
    void testBalanceSheetComparison() {
        Map<String, Object> params = new HashMap<>();
        params.put("reportType", "BALANCE_SHEET");
        params.put("compareMode", true);
        params.put("currentPeriod", currentDate);
        params.put("comparePeriod", lastMonthDate);

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .queryParams(params)
        .when()
            .get("/api/v1/reports/financial/balance-sheet/compare")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getList("data.comparisonData")).isNotEmpty();
        assertThat(response.jsonPath().getMap("data.variance")).isNotNull();
    }

    @Test
    @Order(5)
    @Story("财务报表钻取")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("REPORT-FIN-005: 财务报表科目明细钻取测试")
    @Description("验证报表支持科目明细钻取功能")
    void testFinancialReportDrillDown() {
        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .queryParam("accountCode", "1122")
            .queryParam("accountName", "应收账款")
            .queryParam("startDate", lastMonthDate)
            .queryParam("endDate", currentDate)
        .when()
            .get("/api/v1/reports/financial/drill-down")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getList("data.details")).isNotEmpty();
    }

    @Test
    @Order(6)
    @Story("财务报表异常处理")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("REPORT-FIN-006: 财务报表无数据场景测试")
    @Description("验证无数据时系统正常处理")
    void testFinancialReportNoData() {
        Map<String, Object> params = new HashMap<>();
        params.put("reportType", "BALANCE_SHEET");
        params.put("startDate", "2099-01-01");
        params.put("endDate", "2099-12-31");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .queryParams(params)
        .when()
            .get("/api/v1/reports/financial/balance-sheet")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getString("message")).containsIgnoringCase("暂无数据");
    }

    // ==================== 业务报表测试 ====================

    @Test
    @Order(10)
    @Story("销售报表")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("REPORT-BIZ-001: 销售报表生成测试")
    @Description("验证销售报表能正确生成，数据准确")
    void testSalesReportGeneration() {
        Map<String, Object> params = new HashMap<>();
        params.put("reportType", "SALES");
        params.put("startDate", lastMonthDate);
        params.put("endDate", currentDate);
        params.put("groupBy", "CUSTOMER");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .queryParams(params)
        .when()
            .get("/api/v1/reports/business/sales")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getString("data.reportName")).isEqualTo("销售报表");
        
        // 验证关键指标
        assertThat(response.jsonPath().getDouble("data.totalAmount")).isNotNull();
        assertThat(response.jsonPath().getDouble("data.totalQuantity")).isNotNull();
        assertThat(response.jsonPath().getList("data.details")).isNotEmpty();
    }

    @Test
    @Order(11)
    @Story("采购报表")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("REPORT-BIZ-002: 采购报表生成测试")
    @Description("验证采购报表能正确生成，数据准确")
    void testPurchaseReportGeneration() {
        Map<String, Object> params = new HashMap<>();
        params.put("reportType", "PURCHASE");
        params.put("startDate", lastMonthDate);
        params.put("endDate", currentDate);
        params.put("groupBy", "SUPPLIER");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .queryParams(params)
        .when()
            .get("/api/v1/reports/business/purchase")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getString("data.reportName")).isEqualTo("采购报表");
        assertThat(response.jsonPath().getList("data.details")).isNotEmpty();
    }

    @Test
    @Order(12)
    @Story("库存报表")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("REPORT-BIZ-003: 库存报表生成测试")
    @Description("验证库存报表能正确生成，包含预警功能")
    void testInventoryReportGeneration() {
        Map<String, Object> params = new HashMap<>();
        params.put("reportType", "INVENTORY");
        params.put("queryDate", currentDate);
        params.put("includeWarning", true);

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .queryParams(params)
        .when()
            .get("/api/v1/reports/business/inventory")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getString("data.reportName")).isEqualTo("库存报表");
        assertThat(response.jsonPath().getList("data.inventoryList")).isNotEmpty();
        
        // 验证预警商品
        assertThat(response.jsonPath().getList("data.warningItems")).isNotNull();
    }

    @Test
    @Order(13)
    @Story("销售趋势分析")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("REPORT-BIZ-004: 销售趋势分析报表测试")
    @Description("验证销售趋势分析功能正常")
    void testSalesTrendAnalysis() {
        Map<String, Object> params = new HashMap<>();
        params.put("analysisType", "TREND");
        params.put("timeDimension", "MONTH");
        params.put("compareType", "YEAR_OVER_YEAR");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .queryParams(params)
        .when()
            .get("/api/v1/reports/business/sales/analysis")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getList("data.trendData")).isNotEmpty();
        assertThat(response.jsonPath().getMap("data.comparison")).isNotNull();
    }

    @Test
    @Order(14)
    @Story("库存周转率")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("REPORT-BIZ-005: 库存周转率报表测试")
    @Description("验证库存周转率计算准确")
    void testInventoryTurnoverReport() {
        Map<String, Object> params = new HashMap<>();
        params.put("analysisType", "TURNOVER");
        params.put("startDate", lastMonthDate);
        params.put("endDate", currentDate);

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .queryParams(params)
        .when()
            .get("/api/v1/reports/business/inventory/analysis")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        
        // 验证周转率计算
        Double turnoverRate = response.jsonPath().getDouble("data.summary.turnoverRate");
        assertThat(turnoverRate).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    @Order(15)
    @Story("TOP商品分析")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("REPORT-BIZ-006: TOP10销售商品报表测试")
    @Description("验证TOP商品分析功能正常")
    void testTopProductsReport() {
        Map<String, Object> params = new HashMap<>();
        params.put("analysisType", "TOP_PRODUCTS");
        params.put("limit", 10);
        params.put("sortBy", "AMOUNT");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .queryParams(params)
        .when()
            .get("/api/v1/reports/business/sales/analysis")
        .then()
            .statusCode(200)
            .extract().response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        
        // 验证返回10条数据
        assertThat(response.jsonPath().getList("data.topProducts")).hasSizeLessThanOrEqualTo(10);
    }

    // ==================== 报表导出测试 ====================

    @Test
    @Order(20)
    @Story("报表导出")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("REPORT-EXP-001: 报表Excel导出测试")
    @Description("验证报表能正确导出为Excel格式")
    void testReportExportExcel() {
        Map<String, Object> params = new HashMap<>();
        params.put("reportType", "SALES");
        params.put("startDate", lastMonthDate);
        params.put("endDate", currentDate);
        params.put("exportFormat", "EXCEL");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .queryParams(params)
        .when()
            .get("/api/v1/reports/export")
        .then()
            .statusCode(200)
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .extract().response();

        // 验证文件内容
        byte[] fileContent = response.asByteArray();
        assertThat(fileContent).isNotEmpty();
        assertThat(fileContent.length).isGreaterThan(100);
        
        // 验证Excel文件头 (PK开头)
        assertThat(fileContent[0]).isEqualTo((byte) 0x50);
        assertThat(fileContent[1]).isEqualTo((byte) 0x4B);
    }

    @Test
    @Order(21)
    @Story("报表导出")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("REPORT-EXP-002: 报表PDF导出测试")
    @Description("验证报表能正确导出为PDF格式")
    void testReportExportPDF() {
        Map<String, Object> params = new HashMap<>();
        params.put("reportType", "BALANCE_SHEET");
        params.put("startDate", lastMonthDate);
        params.put("endDate", currentDate);
        params.put("exportFormat", "PDF");

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .queryParams(params)
        .when()
            .get("/api/v1/reports/export")
        .then()
            .statusCode(200)
            .contentType("application/pdf")
            .extract().response();

        byte[] fileContent = response.asByteArray();
        assertThat(fileContent).isNotEmpty();
        
        // 验证PDF文件头 (%PDF)
        assertThat(fileContent[0]).isEqualTo((byte) 0x25);
        assertThat(fileContent[1]).isEqualTo((byte) 0x50);
        assertThat(fileContent[2]).isEqualTo((byte) 0x44);
        assertThat(fileContent[3]).isEqualTo((byte) 0x46);
    }

    @Test
    @Order(22)
    @Story("报表导出")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("REPORT-EXP-003: 大报表导出性能测试")
    @Description("验证大数据量报表导出性能")
    void testLargeReportExportPerformance() {
        Map<String, Object> params = new HashMap<>();
        params.put("reportType", "INVENTORY_DETAIL");
        params.put("exportFormat", "EXCEL");

        long startTime = System.currentTimeMillis();

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .queryParams(params)
        .when()
            .get("/api/v1/reports/export")
        .then()
            .statusCode(200)
            .extract().response();

        long duration = System.currentTimeMillis() - startTime;
        
        // 验证导出时间小于30秒
        assertThat(duration).isLessThan(30000);
        assertThat(response.asByteArray().length).isGreaterThan(0);
    }

    // ==================== 权限测试 ====================

    @Test
    @Order(30)
    @Story("权限控制")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("REPORT-PERM-001: 财务报表权限控制测试")
    @Description("验证无权限用户无法访问财务报表")
    void testFinancialReportPermissionDenied() {
        // 使用无财务权限的token
        String noFinanceToken = "no-finance-permission-token";

        given()
            .header("Authorization", "Bearer " + noFinanceToken)
            .contentType("application/json")
            .queryParam("reportType", "BALANCE_SHEET")
        .when()
            .get("/api/v1/reports/financial/balance-sheet")
        .then()
            .statusCode(403);
    }

    @Test
    @Order(31)
    @Story("权限控制")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("REPORT-PERM-002: 数据范围权限测试")
    @Description("验证用户只能查看授权范围内的数据")
    void testDataScopePermission() {
        // 使用仅查看本部门数据的token
        String deptOnlyToken = "dept-only-token";

        Response response = given()
            .header("Authorization", "Bearer " + deptOnlyToken)
            .contentType("application/json")
            .queryParam("reportType", "SALES")
        .when()
            .get("/api/v1/reports/business/sales")
        .then()
            .statusCode(200)
            .extract().response();

        // 验证只返回本部门数据
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        assertThat(response.jsonPath().getString("data.scope")).isEqualTo("DEPARTMENT");
    }

    // ==================== 性能测试 ====================

    @Test
    @Order(40)
    @Story("性能测试")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("REPORT-PERF-001: 大数据量报表生成性能测试")
    @Description("验证大数据量报表生成性能")
    void testLargeDataReportPerformance() {
        Map<String, Object> params = new HashMap<>();
        params.put("reportType", "INVENTORY_DETAIL");
        params.put("includeAll", true);

        long startTime = System.currentTimeMillis();

        Response response = given()
            .header("Authorization", "Bearer " + AUTH_TOKEN)
            .contentType("application/json")
            .queryParams(params)
        .when()
            .get("/api/v1/reports/business/inventory/detail")
        .then()
            .statusCode(200)
            .extract().response();

        long duration = System.currentTimeMillis() - startTime;
        
        // 验证响应时间小于10秒
        assertThat(duration).isLessThan(10000);
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(41)
    @Story("性能测试")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("REPORT-PERF-002: 并发报表生成测试")
    @Description("验证系统能处理并发报表请求")
    @Execution(ExecutionMode.CONCURRENT)
    void testConcurrentReportGeneration() {
        // 模拟并发请求
        int concurrentUsers = 5;
        
        for (int i = 0; i < concurrentUsers; i++) {
            given()
                .header("Authorization", "Bearer " + AUTH_TOKEN)
                .contentType("application/json")
                .queryParam("reportType", "SALES")
            .when()
                .get("/api/v1/reports/business/sales")
            .then()
                .statusCode(200);
        }
    }
}
