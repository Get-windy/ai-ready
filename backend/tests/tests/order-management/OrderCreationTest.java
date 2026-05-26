package com.aiedge.order.management;

import com.aiedge.api.base.ApiBaseTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Order Creation Functionality Test
 * 
 * This test class specifically focuses on the order creation functionality
 * as required by task-1777153379518-7itchcey.
 * 
 * Test Content:
 * 1. Verify normal order creation flow
 * 2. Test order data validation (required fields, format validation)
 * 3. Verify order status initialization after creation
 * 4. Test exception scenarios (insufficient inventory, price anomalies, etc.)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderCreationTest extends ApiBaseTest {
    private static final String BASE_PATH = "/api/v1/order";

    @Test
    @Order(1)
    @DisplayName("Verify Normal Order Creation Flow")
    void testNormalOrderCreation() {
        // Given: Valid order data
        String requestBody = "{\n" +
                "  \"customerId\": \"cust_001\",\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"productId\": \"prod_001\",\n" +
                "      \"quantity\": 10,\n" +
                "      \"price\": 100\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        
        // When: Creating an order
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post(BASE_PATH);
        
        // Then: Order is created successfully
        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.id", notNullValue())
                .body("data.status", equalTo("created"));
    }

    @Test
    @Order(2)
    @DisplayName("Test Order Data Validation - Required Fields")
    void testDataValidationRequiredFields() {
        // Test missing customerId
        String requestBody1 = "{\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"productId\": \"prod_001\",\n" +
                "      \"quantity\": 10,\n" +
                "      \"price\": 100\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(requestBody1)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo(400));
        
        // Test missing items
        String requestBody2 = "{\n" +
                "  \"customerId\": \"cust_001\"\n" +
                "}";
        
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(requestBody2)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo(400));
        
        // Test empty items array
        String requestBody3 = "{\n" +
                "  \"customerId\": \"cust_001\",\n" +
                "  \"items\": []\n" +
                "}";
        
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(requestBody3)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo(400));
    }

    @Test
    @Order(3)
    @DisplayName("Test Order Data Validation - Format Validation")
    void testDataValidationFormat() {
        // Test invalid quantity (negative)
        String requestBody1 = "{\n" +
                "  \"customerId\": \"cust_001\",\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"productId\": \"prod_001\",\n" +
                "      \"quantity\": -5,\n" +
                "      \"price\": 100\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(requestBody1)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo(400));
        
        // Test invalid price (negative)
        String requestBody2 = "{\n" +
                "  \"customerId\": \"cust_001\",\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"productId\": \"prod_001\",\n" +
                "      \"quantity\": 10,\n" +
                "      \"price\": -100\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(requestBody2)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo(400));
        
        // Test invalid productId format
        String requestBody3 = "{\n" +
                "  \"customerId\": \"cust_001\",\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"productId\": \"\",\n" +
                "      \"quantity\": 10,\n" +
                "      \"price\": 100\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(requestBody3)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo(400));
    }

    @Test
    @Order(4)
    @DisplayName("Verify Order Status Initialization")
    void testOrderStatusInitialization() {
        String requestBody = "{\n" +
                "  \"customerId\": \"cust_001\",\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"productId\": \"prod_001\",\n" +
                "      \"quantity\": 10,\n" +
                "      \"price\": 100\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post(BASE_PATH);
        
        // Verify that the order status is correctly initialized
        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("data.status", equalTo("created"))
                .body("data.createdAt", notNullValue())
                .body("data.updatedAt", notNullValue());
    }

    @Test
    @Order(5)
    @DisplayName("Test Exception Scenario - Insufficient Inventory")
    void testInsufficientInventory() {
        // Assuming product prod_001 has limited inventory
        String requestBody = "{\n" +
                "  \"customerId\": \"cust_001\",\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"productId\": \"prod_001\",\n" +
                "      \"quantity\": 999999, // Requesting more than available inventory
                \"price\": 100\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo(400))
                .body("message", containsString("insufficient"));
    }

    @Test
    @Order(6)
    @DisplayName("Test Exception Scenario - Price Anomaly")
    void testPriceAnomaly() {
        // Test extremely high price
        String requestBody1 = "{\n" +
                "  \"customerId\": \"cust_001\",\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"productId\": \"prod_001\",\n" +
                "      \"quantity\": 1,\n" +
                "      \"price\": 999999999 // Extremely high price\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(requestBody1)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo(400))
                .body("message", containsString("price"));
        
        // Test zero price for non-free products
        String requestBody2 = "{\n" +
                "  \"customerId\": \"cust_001\",\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"productId\": \"prod_001\",\n" +
                "      \"quantity\": 10,\n" +
                "      \"price\": 0 // Zero price for non-free product\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(requestBody2)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo(400))
                .body("message", containsString("price"));
    }
}