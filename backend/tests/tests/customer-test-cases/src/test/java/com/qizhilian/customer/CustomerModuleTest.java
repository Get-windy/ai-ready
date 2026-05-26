package com.qizhilian.customer;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import java.util.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CustomerModuleTest {
    private static final String BASE_URL = System.getProperty("api.base.url", "http://localhost:8080");
    private static final String AUTH_TOKEN = System.getProperty("api.auth.token", "test-token");
    private String customerId;
    private String opportunityId;

    @BeforeAll
    void setUp() { RestAssured.baseURI = BASE_URL; }

    @Test
    @Order(1)
    @DisplayName("CUST-001: 个人客户新增测试")
    void testCreateIndividualCustomer() {
        Map<String, Object> customer = new HashMap<>();
        customer.put("type", "INDIVIDUAL");
        customer.put("name", "张三");
        customer.put("phone", "13800138000");
        customer.put("email", "zhangsan@example.com");
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(customer).when().post("/api/v1/customers").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
        customerId = response.jsonPath().getString("data.id");
    }

    @Test
    @Order(2)
    @DisplayName("CUST-002: 企业客户新增测试")
    void testCreateEnterpriseCustomer() {
        Map<String, Object> customer = new HashMap<>();
        customer.put("type", "ENTERPRISE");
        customer.put("name", "测试科技有限公司");
        customer.put("contact", "李四");
        customer.put("phone", "13900139000");
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(customer).when().post("/api/v1/customers").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(3)
    @DisplayName("CUST-003: 客户信息编辑测试")
    void testEditCustomer() {
        Map<String, Object> update = new HashMap<>();
        update.put("name", "张三-更新");
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(update).when().put("/api/v1/customers/" + customerId).then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(4)
    @DisplayName("CUST-004: 客户分类管理测试")
    void testCustomerCategory() {
        Map<String, Object> category = new HashMap<>();
        category.put("customerId", customerId);
        category.put("category", "VIP");
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(category).when().post("/api/v1/customers/category").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(5)
    @DisplayName("CUST-005: 客户标签管理测试")
    void testCustomerTags() {
        Map<String, Object> tags = new HashMap<>();
        tags.put("customerId", customerId);
        tags.put("tags", Arrays.asList("高价值", "长期合作"));
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(tags).when().post("/api/v1/customers/tags").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(6)
    @DisplayName("CUST-006: 客户导入导出测试")
    void testCustomerImportExport() {
        Response exportResponse = given().header("Authorization", "Bearer " + AUTH_TOKEN).when().get("/api/v1/customers/export").then().statusCode(200).extract().response();
        assertThat(exportResponse.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(7)
    @DisplayName("CUST-007: 客户合并去重测试")
    void testCustomerMerge() {
        Map<String, Object> merge = new HashMap<>();
        merge.put("sourceIds", Arrays.asList("C001", "C002"));
        merge.put("targetId", customerId);
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(merge).when().post("/api/v1/customers/merge").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(10)
    @DisplayName("CUST-LEVEL-001: 等级规则配置测试")
    void testLevelRuleConfig() {
        Map<String, Object> rule = new HashMap<>();
        rule.put("level", "GOLD");
        rule.put("minAmount", 10000);
        rule.put("maxAmount", 50000);
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(rule).when().post("/api/v1/customers/level-rules").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(11)
    @DisplayName("CUST-LEVEL-002: 自动升级测试")
    void testAutoUpgrade() {
        Map<String, Object> upgrade = new HashMap<>();
        upgrade.put("customerId", customerId);
        upgrade.put("triggerAmount", 15000);
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(upgrade).when().post("/api/v1/customers/level-upgrade").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(12)
    @DisplayName("CUST-LEVEL-003: 自动降级测试")
    void testAutoDowngrade() {
        Map<String, Object> downgrade = new HashMap<>();
        downgrade.put("customerId", customerId);
        downgrade.put("reason", "消费金额不足");
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(downgrade).when().post("/api/v1/customers/level-downgrade").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(13)
    @DisplayName("CUST-LEVEL-004: 等级权益验证测试")
    void testLevelBenefits() {
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).pathParam("customerId", customerId).when().get("/api/v1/customers/{customerId}/benefits").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(14)
    @DisplayName("CUST-LEVEL-005: 等级历史记录测试")
    void testLevelHistory() {
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).pathParam("customerId", customerId).when().get("/api/v1/customers/{customerId}/level-history").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200");
    }

    @Test
    @Order(20)
    @DisplayName("CUST-FOLLOW-001: 跟进记录添加测试")
    void testAddFollowUp() {
        Map<String, Object> followUp = new HashMap<>();
        followUp.put("customerId", customerId);
        followUp.put("content", "电话沟通，客户有意向合作");
        followUp.put("followUpType", "PHONE");
        Response response = given().header("Authorization", "Bearer " + AUTH_TOKEN).contentType("application/json").body(followUp).when().post("/api/v1/customers/follow-up").then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("code")).isEqualTo("200"