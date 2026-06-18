package com.qizhilian.api.crm;

import com.qizhilian.api.base.BaseApiTest;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;

/**
 * CRM客户管理接口测试
 */
@Feature("CRM模块")
@Story("客户管理API测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomerApiTest extends BaseApiTest {
    
    private static Long customerId;
    private static Long leadId;
    private static Long opportunityId;
    
    // ==================== 客户管理 ====================
    
    @Test
    @Order(1)
    @DisplayName("创建客户 - 正向测试")
    @Description("创建新客户信息")
    @Severity(SeverityLevel.CRITICAL)
    void testCreateCustomer() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", dataGenerator.generateCustomerName());
        requestBody.put("level", dataGenerator.generateCustomerLevel());
        requestBody.put("industry", "科技");
        requestBody.put("scale", "50-200人");
        requestBody.put("contactName", dataGenerator.generateName());
        requestBody.put("contactPhone", dataGenerator.generatePhone());
        requestBody.put("contactEmail", dataGenerator.generateEmail());
        requestBody.put("address", dataGenerator.generateAddress());
        requestBody.put("source", dataGenerator.generateLeadSource());
        requestBody.put("remark", "测试客户");
        
        Response response = post("/crm/customers", requestBody);
        
        assertStatusCode(response, 201);
        response.then()
            .body("code", equalTo(200))
            .body("data.customerId", notNullValue())
            .body("data.name", equalTo(requestBody.get("name")));
        
        customerId = response.jsonPath().getLong("data.customerId");
    }
    
    @Test
    @Order(2)
    @DisplayName("获取客户列表")
    @Description("分页查询客户列表")
    @Severity(SeverityLevel.NORMAL)
    void testGetCustomerList() {
        Map<String, Object> params = new HashMap<>();
        params.put("page", 1);
        params.put("size", 10);
        params.put("level", "A级");
        
        Response response = get("/crm/customers", params);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("data.list", notNullValue())
            .body("data.total", greaterThanOrEqualTo(0));
    }
    
    @Test
    @Order(3)
    @DisplayName("获取客户详情")
    @Description("根据ID获取客户详细信息")
    @Severity(SeverityLevel.NORMAL)
    void testGetCustomerDetail() {
        if (customerId == null) {
            testCreateCustomer();
        }
        
        Response response = get("/crm/customers/" + customerId);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("data.customerId", equalTo(customerId.intValue()));
    }
    
    @Test
    @Order(4)
    @DisplayName("更新客户信息")
    @Description("更新客户的基本信息")
    @Severity(SeverityLevel.NORMAL)
    void testUpdateCustomer() {
        if (customerId == null) {
            testCreateCustomer();
        }
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("level", "B级");
        requestBody.put("contactName", dataGenerator.generateName());
        requestBody.put("remark", "更新后的备注");
        
        Response response = put("/crm/customers/" + customerId, requestBody);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("message", containsString("成功"));
    }
    
    // ==================== 线索管理 ====================
    
    @Test
    @Order(10)
    @DisplayName("创建线索")
    @Description("创建新的销售线索")
    @Severity(SeverityLevel.CRITICAL)
    void testCreateLead() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("companyName", dataGenerator.generateCompanyName());
        requestBody.put("contactName", dataGenerator.generateName());
        requestBody.put("contactPhone", dataGenerator.generatePhone());
        requestBody.put("contactEmail", dataGenerator.generateEmail());
        requestBody.put("source", dataGenerator.generateLeadSource());
        requestBody.put("requirement", "需要企业管理系统");
        requestBody.put("budget", dataGenerator.generateAmount(10000, 100000));
        requestBody.put("status", "new");
        
        Response response = post("/crm/leads", requestBody);
        
        assertStatusCode(response, 201);
        response.then()
            .body("code", equalTo(200))
            .body("data.leadId", notNullValue());
        
        leadId = response.jsonPath().getLong("data.leadId");
    }
    
    @Test
    @Order(11)
    @DisplayName("获取线索列表")
    @Description("分页查询线索列表")
    @Severity(SeverityLevel.NORMAL)
    void testGetLeadList() {
        Map<String, Object> params = new HashMap<>();
        params.put("page", 1);
        params.put("size", 10);
        params.put("status", "new");
        
        Response response = get("/crm/leads", params);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("data.list", notNullValue());
    }
    
    @Test
    @Order(12)
    @DisplayName("线索转客户")
    @Description("将线索转换为客户")
    @Severity(SeverityLevel.CRITICAL)
    void testConvertLeadToCustomer() {
        if (leadId == null) {
            testCreateLead();
        }
        
        Response response = post("/crm/leads/" + leadId + "/convert", null);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("data.customerId", notNullValue());
        
        customerId = response.jsonPath().getLong("data.customerId");
    }
    
    // ==================== 商机管理 ====================
    
    @Test
    @Order(20)
    @DisplayName("创建商机")
    @Description("创建新的销售商机")
    @Severity(SeverityLevel.CRITICAL)
    void testCreateOpportunity() {
        if (customerId == null) {
            testCreateCustomer();
        }
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("customerId", customerId);
        requestBody.put("name", "企业管理系统项目");
        requestBody.put("stage", dataGenerator.generateOpportunityStage());
        requestBody.put("amount", dataGenerator.generateAmount(50000, 500000));
        requestBody.put("expectedCloseDate", dataGenerator.generateDateTime(30));
        requestBody.put("probability", dataGenerator.randomInt(10, 90));
        requestBody.put("remark", "重要商机");
        
        Response response = post("/crm/opportunities", requestBody);
        
        assertStatusCode(response, 201);
        response.then()
            .body("code", equalTo(200))
            .body("data.opportunityId", notNullValue());
        
        opportunityId = response.jsonPath().getLong("data.opportunityId");
    }
    
    @Test
    @Order(21)
    @DisplayName("获取商机列表")
    @Description("分页查询商机列表")
    @Severity(SeverityLevel.NORMAL)
    void testGetOpportunityList() {
        Map<String, Object> params = new HashMap<>();
        params.put("page", 1);
        params.put("size", 10);
        
        Response response = get("/crm/opportunities", params);
        
        assertStatusCode(response, 200);
        response.then()
            .body("code", equalTo(200))
            .body("data.list", notNullValue());
    }
    
    @Test
    @