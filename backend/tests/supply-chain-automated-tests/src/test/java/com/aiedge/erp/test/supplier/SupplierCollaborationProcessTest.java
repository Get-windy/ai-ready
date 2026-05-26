package com.aiedge.erp.test.supplier;

import com.aiedge.erp.test.framework.BaseIntegrationTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 供应商协同业务流程测试
 * 覆盖供应商注册、资质审核、报价管理等核心业务流程
 */
@Slf4j
@DisplayName("供应商协同业务流程测试")
public class SupplierCollaborationProcessTest extends BaseIntegrationTest {

    // 测试数据常量
    private static final String SUPPLIER_API_PATH = "/suppliers";
    private static final String QUALIFICATION_API_PATH = "/suppliers/{supplierId}/qualifications";
    private static final String QUOTATION_API_PATH = "/quotations";

    @Test
    @DisplayName("测试供应商全生命周期流程")
    void testSupplierFullLifecycleProcess() {
        log.info("开始测试供应商全生命周期流程");

        // 阶段1: 供应商注册
        String supplierId = registerNewSupplier();
        assertSupplierRegistered(supplierId);

        // 阶段2: 资质审核
        String qualificationId = submitSupplierQualification(supplierId);
        approveSupplierQualification(supplierId, qualificationId);
        assertQualificationApproved(supplierId);

        // 阶段3: 报价管理
        String quotationId = createQuotationRequest(supplierId);
        submitSupplierQuotation(supplierId, quotationId);
        evaluateQuotation(quotationId);
        assertQuotationEvaluated(quotationId);

        // 阶段4: 供应商状态管理
        updateSupplierStatus(supplierId, "ACTIVE");
        assertSupplierStatus(supplierId, "ACTIVE");

        log.info("供应商全生命周期流程测试完成");
    }

    @Test
    @DisplayName("测试供应商资质过期流程")
    void testSupplierQualificationExpiryProcess() {
        log.info("开始测试供应商资质过期流程");

        // 创建供应商
        String supplierId = registerNewSupplier();

        // 提交资质（设置过期日期为过去）
        Map<String, Object> qualificationData = new HashMap<>();
        qualificationData.put("qualificationType", "BUSINESS_LICENSE");
        qualificationData.put("documentNumber", "LIC_" + UUID.randomUUID());
        qualificationData.put("expiryDate", "2024-01-01"); // 过期日期
        qualificationData.put("documentFile", "base64_encoded_file_content");

        Response response = given()
                .contentType(ContentType.JSON)
                .pathParam("supplierId", supplierId)
                .body(qualificationData)
                .when()
                .post(QUALIFICATION_API_PATH);

        verifyApiResponse(HttpStatus.CREATED.value(), response);
        String qualificationId = response.jsonPath().getString("id");

        // 批准资质
        Map<String, Object> approvalData = new HashMap<>();
        approvalData.put("approvalStatus", "APPROVED");
        approvalData.put("approvalComments", "资质审核通过");

        given()
                .contentType(ContentType.JSON)
                .pathParam("supplierId", supplierId)
                .pathParam("qualificationId", qualificationId)
                .body(approvalData)
                .when()
                .post("/suppliers/{supplierId}/qualifications/{qualificationId}/approve")
                .then()
                .statusCode(HttpStatus.OK.value());

        // 验证供应商状态（应显示资质过期警告）
        given()
                .pathParam("supplierId", supplierId)
                .when()
                .get("/suppliers/{supplierId}/status")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("qualificationStatus", equalTo("EXPIRED"))
                .body("warnings", hasItem(containsString("资质已过期")));

        log.info("供应商资质过期流程测试完成");
    }

    @Test
    @DisplayName("测试供应商报价竞争流程")
    void testSupplierQuotationCompetitionProcess() {
        log.info("开始测试供应商报价竞争流程");

        // 创建三个供应商
        String supplier1 = registerNewSupplier();
        String supplier2 = registerNewSupplier();
        String supplier3 = registerNewSupplier();

        // 创建询价单
        Map<String, Object> quotationRequest = new HashMap<>();
        quotationRequest.put("itemCode", "ITEM_" + System.currentTimeMillis());
        quotationRequest.put("itemName", "测试物料");
        quotationRequest.put("quantity", 100);
        quotationRequest.put("unit", "PC");
        quotationRequest.put("deliveryDate", "2024-12-31");

        Response requestResponse = given()
                .contentType(ContentType.JSON)
                .body(quotationRequest)
                .when()
                .post(QUOTATION_API_PATH + "/requests");

        verifyApiResponse(HttpStatus.CREATED.value(), requestResponse);
        String quotationRequestId = requestResponse.jsonPath().getString("id");

        // 三个供应商分别报价
        submitQuotationForSupplier(supplier1, quotationRequestId, 100.0, 5);  // 价格100，交期5天
        submitQuotationForSupplier(supplier2, quotationRequestId, 95.0, 7);   // 价格95，交期7天
        submitQuotationForSupplier(supplier3, quotationRequestId, 110.0, 3);  // 价格110，交期3天

        // 评估报价
        given()
                .pathParam("requestId", quotationRequestId)
                .when()
                .post(QUOTATION_API_PATH + "/requests/{requestId}/evaluate")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("bestQuotation.supplierId", equalTo(supplier2))  // 价格最低的供应商
                .body("quotations", hasSize(3));

        log.info("供应商报价竞争流程测试完成");
    }

    // ========== 私有辅助方法 ==========

    private String registerNewSupplier() {
        Map<String, Object> supplierData = new HashMap<>();
        supplierData.put("name", "测试供应商_" + createUniqueIdentifier("SUP"));
        supplierData.put("code", "SUP_" + System.currentTimeMillis());
        supplierData.put("type", "MANUFACTURER");
        supplierData.put("contactPerson", "张测试");
        supplierData.put("contactPhone", "13800138000");
        supplierData.put("contactEmail", "test@example.com");
        supplierData.put("address", "测试地址");

        Response response = given()
                .contentType(ContentType.JSON)
                .body(supplierData)
                .when()
                .post(SUPPLIER_API_PATH);

        verifyApiResponse(HttpStatus.CREATED.value(), response);
        String supplierId = response.jsonPath().getString("id");
        log.info("注册新供应商成功，ID: {}", supplierId);
        return supplierId;
    }

    private void assertSupplierRegistered(String supplierId) {
        given()
                .pathParam("supplierId", supplierId)
                .when()
                .get(SUPPLIER_API_PATH + "/{supplierId}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(supplierId))
                .body("status", equalTo("REGISTERED"));
    }

    private String submitSupplierQualification(String supplierId) {
        Map<String, Object> qualificationData = new HashMap<>();
        qualificationData.put("qualificationType", "BUSINESS_LICENSE");
        qualificationData.put("documentNumber", "LIC_" + UUID.randomUUID());
        qualificationData.put("expiryDate", "2025-12-31");
        qualificationData.put("documentFile", "base64_encoded_file_content");

        Response response = given()
                .contentType(ContentType.JSON)
                .pathParam("supplierId", supplierId)
                .body(qualificationData)
                .when()
                .post(QUALIFICATION_API_PATH);

        verifyApiResponse(HttpStatus.CREATED.value(), response);
        String qualificationId = response.jsonPath().getString("id");
        log.info("提交供应商资质成功，资质ID: {}", qualificationId);
        return qualificationId;
    }

    private void approveSupplierQualification(String supplierId, String qualificationId) {
        Map<String, Object> approvalData = new HashMap<>();
        approvalData.put("approvalStatus", "APPROVED");
        approvalData.put("approvalComments", "资质材料完整，符合要求");

        given()
                .contentType(ContentType.JSON)
                .pathParam("supplierId", supplierId)
                .pathParam("qualificationId", qualificationId)
                .body(approvalData)
                .when()
                .post("/suppliers/{supplierId}/qualifications/{qualificationId}/approve")
                .then()
                .statusCode(HttpStatus.OK.value());

        log.info("批准供应商资质成功");
    }

    private void assertQualificationApproved(String supplierId) {
        given()
                .pathParam("supplierId", supplierId)
                .when()
                .get(SUPPLIER_API_PATH + "/{supplierId}/qualifications/status")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("latestQualification.status", equalTo("APPROVED"));
    }

    private String createQuotationRequest(String supplierId) {
        Map<String, Object> quotationRequest = new HashMap<>();
        quotationRequest.put("supplierId", supplierId);
        quotationRequest.put("itemCode", "ITEM_" + System.currentTimeMillis());
        quotationRequest.put("itemName", "测试物料");
        quotationRequest.put("quantity", 50);
        quotationRequest.put("unit", "PC");
        quotationRequest.put("requiredDate", "2024-12-31");

        Response response = given()
                .contentType(ContentType.JSON)
                .body(quotationRequest)
                .when()
                .post(QUOTATION_API_PATH + "/requests");

        verifyApiResponse(HttpStatus.CREATED.value(), response);
        String quotationId = response.jsonPath().getString("id");
        log.info("创建询价单成功，ID: {}", quotationId);
        return quotationId;
    }

    private void submitSupplierQuotation(String supplierId, String quotationId) {
        Map<String, Object> quotationData = new HashMap<>();
        quotationData.put("supplierId", supplierId);
        quotationData.put("unitPrice", 100.0);
        quotationData.put("currency", "CNY");
        quotationData.put("deliveryDays", 10);
        quotationData.put("paymentTerms", "NET30");
        quotationData.put("validUntil", "2024-12-31");

        given()
                .contentType(ContentType.JSON)
                .pathParam("quotationId", quotationId)
                .body(quotationData)
                .when()
                .post(QUOTATION_API_PATH + "/{quotationId}/submit")
                .then()
                .statusCode(HttpStatus.CREATED.value());

        log.info("提交供应商报价成功");
    }

    private void evaluateQuotation(String quotationId) {
        Map<String, Object> evaluationData = new HashMap<>();
        evaluationData.put("evaluationResult", "ACCEPTED");
        evaluationData.put("evaluationComments", "价格合理，交期可接受");
        evaluationData.put("evaluatedBy", "test_evaluator");

        given()
                .contentType(ContentType.JSON)
                .pathParam("quotationId", quotationId)
                .body(evaluationData)
                .when()
                .post(QUOTATION_API_PATH + "/{quotationId}/evaluate")
                .then()
                .statusCode(HttpStatus.OK.value());

        log.info("评估报价成功");
    }

    private void assertQuotationEvaluated(String quotationId) {
        given()
                .pathParam("quotationId", quotationId)
                .when()
                .get(QUOTATION_API_PATH + "/{quotationId}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("EVALUATED"))
                .body("evaluationResult", equalTo("ACCEPTED"));
    }

    private void updateSupplierStatus(String supplierId, String status) {
        Map<String, Object> statusData = new HashMap<>();
        statusData.put("status", status);
        statusData.put("reason", "测试状态更新");

        given()
                .contentType(ContentType.JSON)
                .pathParam("supplierId", supplierId)
                .body(statusData)
                .when()
                .put(SUPPLIER_API_PATH + "/{supplierId}/status")
                .then()
                .statusCode(HttpStatus.OK.value());

        log.info("更新供应商状态为: {}", status);
    }

    private void assertSupplierStatus(String supplierId, String expectedStatus) {
        given()
                .pathParam("supplierId", supplierId)
                .when()
                .get(SUPPLIER_API_PATH + "/{supplierId}/status")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("currentStatus", equalTo(expectedStatus));
    }

    private void submitQuotationForSupplier(String supplierId, String quotationRequestId, double price, int deliveryDays) {
        Map<String, Object> quotationData = new HashMap<>();
        quotationData.put("supplierId", supplierId);
        quotationData.put("unitPrice", price);
        quotationData.put("currency", "CNY");
        quotationData.put("deliveryDays", deliveryDays);
        quotationData.put("paymentTerms", "NET30");

        given()
                .contentType(ContentType.JSON)
                .pathParam("requestId", quotationRequestId)
                .body(quotationData)
                .when()
                .post(QUOTATION_API_PATH + "/requests/{requestId}/quotations")
                .then()
                .statusCode(HttpStatus.CREATED.value());

        log.info("供应商 {} 报价提交成功，价格: {}, 交期: {}天", supplierId, price, deliveryDays);
    }

    @Override
    protected void cleanupTestData() {
        log.debug("清理供应商测试数据...");
        // 在实际项目中，这里会清理测试创建的供应商数据
        // 例如：调用清理API或直接操作数据库
    }
}