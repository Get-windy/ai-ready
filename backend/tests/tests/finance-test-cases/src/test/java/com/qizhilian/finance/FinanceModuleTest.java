package com.qizhilian.finance;

import com.qizhilian.api.base.ApiBaseTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 财务模块API测试
 * 
 * 测试覆盖：
 * - 应收管理测试（应收账款登记/收款单处理/账龄分析/坏账处理）
 * - 应付管理测试（应付账款登记/付款单处理/付款计划/供应商对账）
 * - 总账管理测试（凭证录入/凭证审核/科目余额/期末结账）
 * - 财务报表测试（资产负债表/利润表/现金流量表/科目明细账）
 * - 权限测试（不同角色操作权限/跨期修改权限）
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FinanceModuleTest extends ApiBaseTest {

    private static final String BASE_PATH = "/api/v1/finance";

    // ==================== 应收管理测试 ====================
    @Test
    @Order(1)
    @DisplayName("应收账款登记测试")
    void testCreateReceivable() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"customerId\": \"cust_123456\",\n" +
                        "  \"orderId\": \"so_123456\",\n" +
                        "  \"amount\": 50000.00,\n" +
                        "  \"dueDate\": \"2026-05-14\",\n" +
                        "  \"description\": \"销售订单应收账款\",\n" +
                        "  \"invoiceNo\": \"INV20260414001\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/receivable")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.id", notNullValue())
                .body("data.status", equalTo("unpaid"))
                .body("data.amount", equalTo(50000.00f))
                .extract().response();

        String receivableId = response.jsonPath().getString("data.id");
        Assertions.assertNotNull(receivableId, "应收账款ID不应为空");
    }

    @Test
    @Order(2)
    @DisplayName("收款单处理测试")
    void testReceivePayment() {
        // 先创建应收账款
        Response createResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"customerId\": \"cust_123456\",\n" +
                        "  \"amount\": 30000.00,\n" +
                        "  \"dueDate\": \"2026-05-14\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/receivable")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().response();

        String receivableId = createResponse.jsonPath().getString("data.id");

        // 执行收款
        Response paymentResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"receivableId\": \"" + receivableId + "\",\n" +
                        "  \"amount\": 30000.00,\n" +
                        "  \"paymentMethod\": \"bank_transfer\",\n" +
                        "  \"paymentDate\": \"2026-04-14\",\n" +
                        "  \"remark\": \"全额收款\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/receivable/" + receivableId + "/payment")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.status", equalTo("paid"))
                .extract().response();
    }

    @Test
    @Order(3)
    @DisplayName("账龄分析测试")
    void testAgingAnalysis() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("startDate", "2026-01-01")
                .param("endDate", "2026-04-14")
                .param("dimension", "customer")
                .when()
                .get(BASE_PATH + "/receivable/aging")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data", notNullValue())
                .extract().response();
    }

    @Test
    @Order(4)
    @DisplayName("坏账处理测试")
    void testBadDebtWriteOff() {
        // 先创建应收账款
        Response createResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"customerId\": \"cust_123456\",\n" +
                        "  \"amount\": 10000.00,\n" +
                        "  \"dueDate\": \"2026-01-01\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/receivable")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().response();

        String receivableId = createResponse.jsonPath().getString("data.id");

        // 申请坏账
        Response badDebtResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"receivableId\": \"" + receivableId + "\",\n" +
                        "  \"reason\": \"客户破产，无法收回\",\n" +
                        "  \"writeOffAmount\": 10000.00\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/receivable/" + receivableId + "/bad-debt")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(5)
    @DisplayName("应收账款超额收款测试")
    void testOverPayment() {
        // 先创建应收账款
        Response createResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"customerId\": \"cust_123456\",\n" +
                        "  \"amount\": 20000.00,\n" +
                        "  \"dueDate\": \"2026-05-14\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/receivable")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().response();

        String receivableId = createResponse.jsonPath().getString("data.id");

        // 超额收款
        Response overPayResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"receivableId\": \"" + receivableId + "\",\n" +
                        "  \"amount\": 25000.00,\n" +
                        "  \"paymentMethod\": \"bank_transfer\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/receivable/" + receivableId + "/payment")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.overpayment", equalTo(5000.00f))
                .extract().response();
    }

    // ==================== 应付管理测试 ====================
    @Test
    @Order(6)
    @DisplayName("应付账款登记测试")
    void testCreatePayable() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"supplierId\": \"sup_123456\",\n" +
                        "  \"orderId\": \"po_123456\",\n" +
                        "  \"amount\": 80000.00,\n" +
                        "  \"dueDate\": \"2026-06-14\",\n" +
                        "  \"description\": \"采购订单应付账款\",\n" +
                        "  \"invoiceNo\": \"POINV20260414001\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/payable")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.id", notNullValue())
                .body("data.status", equalTo("unpaid"))
                .body("data.amount", equalTo(80000.00f))
                .extract().response();

        String payableId = response.jsonPath().getString("data.id");
        Assertions.assertNotNull(payableId, "应付账款ID不应为空");
    }

    @Test
    @Order(7)
    @DisplayName("付款单处理测试")
    void testMakePayment() {
        // 先创建应付账款
        Response createResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"supplierId\": \"sup_123456\",\n" +
                        "  \"amount\": 40000.00,\n" +
                        "  \"dueDate\": \"2026-06-14\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/payable")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().response();

        String payableId = createResponse.jsonPath().getString("data.id");

        // 执行付款
        Response paymentResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"payableId\": \"" + payableId + "\",\n" +
                        "  \"amount\": 40000.00,\n" +
                        "  \"paymentMethod\": \"bank_transfer\",\n" +
                        "  \"paymentDate\": \"2026-04-14\",\n" +
                        "  \"remark\": \"全额付款\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/payable/" + payableId + "/pay")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.status", equalTo("paid"))
                .extract().response();
    }

    @Test
    @Order(8)
    @DisplayName("付款计划测试")
    void testPaymentPlan() {
        // 先创建应付账款
        Response createResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"supplierId\": \"sup_123456\",\n" +
                        "  \"amount\": 60000.00,\n" +
                        "  \"dueDate\": \"2026-07-14\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/payable")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().response();

        String payableId = createResponse.jsonPath().getString("data.id");

        // 创建付款计划
        Response planResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"payableId\": \"" + payableId + "\",\n" +
                        "  \"installments\": [\n" +
                        "    {\"amount\": 20000.00, \"dueDate\": \"2026-05-14\"},\n" +
                        "    {\"amount\": 20000.00, \"dueDate\": \"2026-06-14\"},\n" +
                        "    {\"amount\": 20000.00, \"dueDate\": \"2026-07-14\"}\n" +
                        "  ]\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/payable/" + payableId + "/plan")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.installments", hasSize(3))
                .extract().response();
    }

    @Test
    @Order(9)
    @DisplayName("供应商对账测试")
    void testSupplierReconciliation() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("supplierId", "sup_123456")
                .param("startDate", "2026-01-01")
                .param("endDate", "2026-04-14")
                .when()
                .get(BASE_PATH + "/payable/reconciliation")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.supplierId", equalTo("sup_123456"))
                .body("data.balance", notNullValue())
                .extract().response();
    }

    // ==================== 总账管理测试 ====================
    @Test
    @Order(10)
    @DisplayName("凭证录入测试")
    void testCreateVoucher() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"voucherDate\": \"2026-04-14\",\n" +
                        "  \"voucherWord\": \"记\",\n" +
                        "  \"attachments\": 1,\n" +
                        "  \"entries\": [\n" +
                        "    {\n" +
                        "      \"accountCode\": \"1001\",\n" +
                        "      \"accountName\": \"银行存款\",\n" +
                        "      \"direction\": \"debit\",\n" +
                        "      \"amount\": 50000.00,\n" +
                        "      \"remark\": \"收到货款\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"accountCode\": \"1122\",\n" +
                        "      \"accountName\": \"应收账款\",\n" +
                        "      \"direction\": \"credit\",\n" +
                        "      \"amount\": 50000.00,\n" +
                        "      \"remark\": \"收到货款\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/voucher")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.id", notNullValue())
                .body("data.status", equalTo("draft"))
                .body("data.isBalanced", equalTo(true))
                .extract().response();

        String voucherId = response.jsonPath().getString("data.id");
        Assertions.assertNotNull(voucherId, "凭证ID不应为空");
    }

    @Test
    @Order(11)
    @DisplayName("凭证审核测试")
    void testApproveVoucher() {
        // 先创建凭证
        Response createResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"voucherDate\": \"2026-04-14\",\n" +
                        "  \"voucherWord\": \"记\",\n" +
                        "  \"entries\": [\n" +
                        "    {\"accountCode\": \"1001\", \"direction\": \"debit\", \"amount\": 10000.00},\n" +
                        "    {\"accountCode\": \"4001\", \"direction\": \"credit\", \"amount\": 10000.00}\n" +
                        "  ]\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/voucher")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().response();

        String voucherId = createResponse.jsonPath().getString("data.id");

        // 审核凭证
        Response approveResponse = given()
                .header("Authorization", "Bearer " + mainUserToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"voucherId\": \"" + voucherId + "\",\n" +
                        "  \"approved\": true,\n" +
                        "  \"comment\": \"凭证符合规范，同意审核\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/voucher/" + voucherId + "/approve")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.status", equalTo("approved"))
                .extract().response();
    }

    @Test
    @Order(12)
    @DisplayName("凭证借贷不平衡测试")
    void testUnbalancedVoucher() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"voucherDate\": \"2026-04-14\",\n" +
                        "  \"voucherWord\": \"记\",\n" +
                        "  \"entries\": [\n" +
                        "    {\"accountCode\": \"1001\", \"direction\": \"debit\", \"amount\": 100.00},\n" +
                        "    {\"accountCode\": \"4001\", \"direction\": \"credit\", \"amount\": 50.00}\n" +
                        "  ]\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/voucher")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", not(equalTo(0)))
                .body("message", containsString("借贷不平衡"));
    }

    @Test
    @Order(13)
    @DisplayName("科目余额查询测试")
    void testAccountBalance() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("period", "2026-04")
                .param("accountCode", "1001")
                .when()
                .get(BASE_PATH + "/account/balance")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.accountCode", equalTo("1001"))
                .body("data.openingBalance", notNullValue())
                .body("data.debitTotal", notNullValue())
                .body("data.creditTotal", notNullValue())
                .body("data.closingBalance", notNullValue())
                .extract().response();
    }

    @Test
    @Order(14)
    @DisplayName("期末结账测试")
    void testPeriodClose() {
        Response response = given()
                .header("Authorization", "Bearer " + mainUserToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"period\": \"2026-03\",\n" +
                        "  \"forceClose\": false\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/period/close")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.status", equalTo("closed"))
                .extract().response();
    }

    @Test
    @Order(15)
    @DisplayName("反结账测试")
    void testPeriodUnclose() {
        Response response = given()
                .header("Authorization", "Bearer " + mainUserToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"period\": \"2026-03\",\n" +
                        "  \"reason\": \"调整凭证需要重新入账\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/period/unclose")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.status", equalTo("open"))
                .extract().response();
    }

    // ==================== 财务报表测试 ====================
    @Test
    @Order(16)
    @DisplayName("资产负债表测试")
    void testBalanceSheet() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("period", "2026-03")
                .when()
                .get(BASE_PATH + "/report/balance-sheet")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.period", equalTo("2026-03"))
                .body("data.assets", notNullValue())
                .body("data.liabilities", notNullValue())
                .body("data.equity", notNullValue())
                .body("data.isBalanced", equalTo(true))
                .extract().response();
    }

    @Test
    @Order(17)
    @DisplayName("利润表测试")
    void testIncomeStatement() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("period", "2026-03")
                .when()
                .get(BASE_PATH + "/report/income-statement")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.period", equalTo("2026-03"))
                .body("data.revenue", notNullValue())
                .body("data.expenses", notNullValue())
                .body("data.profit", notNullValue())
                .extract().response();
    }

    @Test
    @Order(18)
    @DisplayName("现金流量表测试")
    void testCashFlowStatement() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("period", "2026-03")
                .when()
                .get(BASE_PATH + "/report/cash-flow")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.period", equalTo("2026-03"))
                .body("data.operating", notNullValue())
                .body("data.investing", notNullValue())
                .body("data.financing", notNullValue())
                .extract().response();
    }

    @Test
    @Order(19)
    @DisplayName("科目明细账测试")
    void testAccountDetail() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("accountCode", "1001")
                .param("startDate", "2026-01-01")
                .param("endDate", "2026-04-14")
                .when()
                .get(BASE_PATH + "/report/account-detail")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.accountCode", equalTo("1001"))
                .body("data.vouchers", notNullValue())
                .extract().response();
    }

    // ==================== 权限测试 ====================
    @Test
    @Order(20)
    @DisplayName("不同角色财务权限测试")
    void testFinanceRolePermissions() {
        // 财务员角色尝试审核凭证
        Response clerkResponse = given()
                .header("Authorization", "Bearer " + clerkToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"voucherId\": \"voucher_123\",\n" +
                        "  \"approved\": true\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/voucher/voucher_123/approve")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("code", not(equalTo(0)))
                .extract().response();
    }

    @Test
    @Order(21)
    @DisplayName("跨期凭证修改权限测试")
    void testCrossPeriodModify() {
        // 尝试修改已结账期间凭证
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"entries\": [\n" +
                        "    {\"accountCode\": \"1001\", \"direction\": \"debit\", \"amount\": 200.00},\n" +
                        "    {\"accountCode\": \"4001\", \"direction\": \"credit\", \"amount\": 200.00}\n" +
                        "  ]\n" +
                        "}")
                .when()
                .put(BASE_PATH + "/voucher/voucher_202601/period/2026-01")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", containsString("已结账期间"))
                .extract().response();
    }

    // ==================== 异常测试 ====================
    @Test
    @Order(22)
    @DisplayName("未授权访问财务接口测试")
    void testUnauthorizedFinanceAccess() {
        given()
                .when()
                .get(BASE_PATH + "/receivable")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @Order(23)
    @DisplayName("无效科目代码测试")
    void testInvalidAccountCode() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .param("accountCode", "invalid_code_999")
                .when()
                .get(BASE_PATH + "/account/balance")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", not(equalTo(0)));
    }
}