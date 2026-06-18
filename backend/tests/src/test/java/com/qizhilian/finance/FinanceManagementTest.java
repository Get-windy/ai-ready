package com.qizhilian.finance;

import com.qizhilian.api.base.ApiBaseTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FinanceManagementTest extends ApiBaseTest {
    private static final String BASE_PATH = "/api/v1/finance";

    @Test @Order(1) @DisplayName("账单创建测试")
    void testCreateBill() {
        given().header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("{\"customerId\": \"cust_001\", \"amount\": 1000, \"type\": \"income\"}")
            .when().post(BASE_PATH + "/bill")
            .then().statusCode(HttpStatus.OK.value()).body("code", equalTo(0));
    }

    @Test @Order(2) @DisplayName("账单查询测试")
    void testQueryBills() {
        given().header("Authorization", "Bearer " + accessToken)
            .param("page", 1).param("size", 10)
            .when().get(BASE_PATH + "/bills")
            .then().statusCode(HttpStatus.OK.value()).body("code", equalTo(0));
    }

    @Test @Order(3) @DisplayName("账单支付测试")
    void testPayBill() {
        Response r = given().header("Authorization", "Bearer " + accessToken)
            .contentType("application/json").body("{\"customerId\": \"cust_001\", \"amount\": 500}")
            .when().post(BASE_PATH + "/bill").then().extract().response();
        String billId = r.jsonPath().getString("data.id");
        given().header("Authorization", "Bearer " + accessToken)
            .contentType("application/json").body("{\"paymentMethod\": \"alipay\"}")
            .when().post(BASE_PATH + "/bill/" + billId + "/pay")
            .then().statusCode(HttpStatus.OK.value()).body("code", equalTo(0));
    }

    @Test @Order(4) @DisplayName("财务报表生成测试")
    void testGenerateReport() {
        given().header("Authorization", "Bearer " + accessToken)
            .param("startDate", "2026-04-01")
            .param("endDate", "2026-04-30")
            .when().get(BASE_PATH + "/report")
            .then().statusCode(HttpStatus.OK.value()).body("code", equalTo(0));
    }

    @Test @Order(5) @DisplayName("收入统计测试")
    void testIncomeStatistics() {
        given().header("Authorization", "Bearer " + accessToken)
            .param("period", "month")
            .when().get(BASE_PATH + "/statistics/income")
            .then().statusCode(HttpStatus.OK.value()).body("code", equalTo(0));
    }

    @Test @Order(6) @DisplayName("支出统计测试")
    void testExpenseStatistics() {
        given().header("Authorization", "Bearer " + accessToken)
            .param("period", "month")
            .when().get(BASE_PATH + "/statistics/expense")
            .then().statusCode(HttpStatus.OK.value()).body("code", equalTo(0));
    }

    @Test @Order(7) @DisplayName("未授权访问测试")
    void testUnauthorizedAccess() {
        given().when().get(BASE_PATH + "/bills")
            .then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test @Order(8) @DisplayName("金额边界测试")
    void testAmountBoundary() {
        given().header("Authorization", "Bearer " + accessToken)
            .contentType("application/json")
            .body("{\"amount\": -100}")
            .when().post(BASE_PATH + "/bill")
            .then().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test @Order(9) @DisplayName("财务数据一致性测试")
    void testDataConsistency() {
        given().header("Authorization", "Bearer " + accessToken)
            .when().get(BASE_PATH + "/consistency/check")
            .then().statusCode(HttpStatus.OK.value()).body("code", equalTo(0));
    }

    @Test @Order(10) @DisplayName("账单详情查询测试")
    void testBillDetail() {
        Response r = given().header("Authorization", "Bearer " + accessToken)
            .contentType("application/json").body("{\"customerId\": \"cust_001\", \"amount\": 200}")
            .when().post(BASE_PATH + "/bill").then().extract().response();
        String billId = r.jsonPath().getString("data.id");
        given().header("Authorization", "Bearer " + accessToken)
            .when().get(BASE_PATH + "/bill/" + billId)
            .then().statusCode(HttpStatus.OK.value()).body("code", equalTo(0));
    }
}