package com.qizhilian.delivery;

import com.qizhilian.api.base.ApiBaseTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 配送模块API测试
 * 
 * 测试覆盖：
 * - 配送员管理测试（注册/认证/档案/排班/绩效）
 * - 配送任务管理测试（任务分配/状态流转/重新分配/合并/拆分/取消）
 * - 配送路线优化测试（高德API/多点路径/实时路况/导航）
 * - 配送跟踪测试（GPS定位/进度跟踪/异常处理/客户通知）
 * - 电子签收测试（电子签名/拍照签收/签收记录/配送评价）
 * - 权限测试（角色权限/数据隔离）
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DeliveryModuleTest extends ApiBaseTest {

    private static final String BASE_PATH = "/api/v1/delivery";

    // ==================== 配送员管理测试 ====================
    @Test
    @Order(1)
    @DisplayName("配送员注册测试")
    void testDriverRegistration() {
        Response response = given()
                .contentType("application/json")
                .body("{\n" +
                        "  \"name\": \"张三\",\n" +
                        "  \"phone\": \"13800138000\",\n" +
                        "  \"driveLicense\": \"C123456789\",\n" +
                        "  \"address\": \"北京市朝阳区测试街道123号\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/drivers/register")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.status", equalTo("pending_review"))
                .extract().response();

        String driverId = response.jsonPath().getString("data.id");
        Assertions.assertNotNull(driverId, "配送员ID不应为空");
    }

    @Test
    @Order(2)
    @DisplayName("配送员认证测试")
    void testDriverCertification() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .multiPart("file", "id_card.jpg", "data".getBytes())
                .multiPart("type", "id_card")
                .when()
                .post(BASE_PATH + "/drivers/certification/upload")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.fileName", notNullValue())
                .extract().response();
    }

    @Test
    @Order(3)
    @DisplayName("配送员档案管理测试")
    void testDriverProfile() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/drivers/profile")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.name", notNullValue())
                .body("data.phone", notNullValue())
                .extract().response();
    }

    @Test
    @Order(4)
    @DisplayName("排班管理测试")
    void testSchedule() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"driverId\": \"driver_123456\",\n" +
                        "  \"date\": \"2026-04-14\",\n" +
                        "  \"shift\": \"morning\",\n" +
                        "  \"startTime\": \"08:00\",\n" +
                        "  \"endTime\": \"12:00\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/schedule")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(5)
    @DisplayName("绩效考核测试")
    void testPerformance() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("driverId", "driver_123456")
                .param("period", "2026-03")
                .when()
                .get(BASE_PATH + "/performance")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.ordersCompleted", notNullValue())
                .body("data.satisfaction", notNullValue())
                .extract().response();
    }

    @Test
    @Order(6)
    @DisplayName("配送员重复手机号注册测试")
    void testDuplicatePhone() {
        given()
                .contentType("application/json")
                .body("{\n" +
                        "  \"name\": \"李四\",\n" +
                        "  \"phone\": \"13800138000\",\n" +
                        "  \"driveLicense\": \"C987654321\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/drivers/register")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", not(equalTo(0)))
                .body("message", containsString("已存在"));
    }

    // ==================== 配送任务管理测试 ====================
    @Test
    @Order(7)
    @DisplayName("任务分配策略测试")
    void testTaskAssignment() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/tasks/assign")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.assignedTasks", notNullValue())
                .extract().response();
    }

    @Test
    @Order(8)
    @DisplayName("任务状态流转测试")
    void testTaskStatusFlow() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/tasks/status")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.tasks", notNullValue())
                .extract().response();
    }

    @Test
    @Order(9)
    @DisplayName("任务重新分配测试")
    void testTaskReroute() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"taskId\": \"task_123456\",\n" +
                        "  \"newDriverId\": \"driver_654321\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/tasks/reroute")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(10)
    @DisplayName("任务合并测试")
    void testTaskMerge() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"taskIds\": [\"task_111\", \"task_222\"],\n" +
                        "  \"newTaskName\": \"合并任务\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/tasks/merge")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(11)
    @DisplayName("任务拆分测试")
    void testTaskSplit() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"taskId\": \"task_123456\",\n" +
                        "  \"splitItems\": [\n" +
                        "    {\"itemId\": \"item_1\", \"quantity\": 2},\n" +
                        "    {\"itemId\": \"item_2\", \"quantity\": 3}\n" +
                        "  ]\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/tasks/split")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(12)
    @DisplayName("任务取消测试")
    void testTaskCancel() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"taskId\": \"task_123456\",\n" +
                        "  \"reason\": \"客户取消订单\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/tasks/cancel")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    // ==================== 配送路线优化测试 ====================
    @Test
    @Order(13)
    @DisplayName("高德API集成测试")
    void testGaodeApiIntegration() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"origin\": \"北京市朝阳区起点\",\n" +
                        "  \"destination\": \"北京市海淀区终点\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/routes/gaode")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.distance", notNullValue())
                .body("data.duration", notNullValue())
                .extract().response();
    }

    @Test
    @Order(14)
    @DisplayName("多点路径规划测试")
    void testMultiPointRouting() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"points\": [\n" +
                        "    \"北京市朝阳区点1\",\n" +
                        "    \"北京市海淀区点2\",\n" +
                        "    \"北京市丰台区点3\"\n" +
                        "  ]\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/routes/multi-point")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.bestRoute", notNullValue())
                .extract().response();
    }

    @Test
    @Order(15)
    @DisplayName("实时路况调整测试")
    void testRealTimeTraffic() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"taskId\": \"task_123456\",\n" +
                        "  \"trafficData\": {\"congestion\": \"heavy\", \"estimatedDelay\": 15}\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/routes/traffic-update")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(16)
    @DisplayName("导航功能测试")
    void testNavigation() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"taskId\": \"task_123456\",\n" +
                        "  \"destination\": \"北京市海淀区终点\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/navigation/start")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    // ==================== 配送跟踪测试 ====================
    @Test
    @Order(17)
    @DisplayName("GPS实时定位测试")
    void testGpsLocation() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"driverId\": \"driver_123456\",\n" +
                        "  \"lat\": 39.9042,\n" +
                        "  \"lng\": 116.4074\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/location/update")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(18)
    @DisplayName("配送进度跟踪测试")
    void testDeliveryProgress() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("taskId", "task_123456")
                .when()
                .get(BASE_PATH + "/progress")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.status", notNullValue())
                .body("data.steps", notNullValue())
                .extract().response();
    }

    @Test
    @Order(19)
    @DisplayName("异常处理测试")
    void testExceptionHandling() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"taskId\": \"task_123456\",\n" +
                        "  \"exceptionType\": \"customer_not_home\",\n" +
                        "  \"remark\": \"客户不在家\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/exception/report")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(20)
    @DisplayName("客户通知测试")
    void testCustomerNotification() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"taskId\": \"task_123456\",\n" +
                        "  \"message\": \"您的订单已发货，预计30分钟后送达\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/notification/send")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    // ==================== 电子签收测试 ====================
    @Test
    @Order(21)
    @DisplayName("电子签名测试")
    void testDigitalSignature() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"taskId\": \"task_123456\",\n" +
                        "  \"signatureBase64\": \"data:image/png;base64,iVARCHAR(1000)\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/signature/submit")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(22)
    @DisplayName("拍照签收测试")
    void testPhotoSignature() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .multiPart("file", "signature.jpg", "data".getBytes())
                .when()
                .post(BASE_PATH + "/signature/photo")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.imageUrl", notNullValue())
                .extract().response();
    }

    @Test
    @Order(23)
    @DisplayName("签收记录查询测试")
    void testSignatureRecords() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("taskId", "task_123456")
                .when()
                .get(BASE_PATH + "/signature/records")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.records", notNullValue())
                .extract().response();
    }

    @Test
    @Order(24)
    @DisplayName("配送评价测试")
    void testDeliveryRating() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"taskId\": \"task_123456\",\n" +
                        "  \"rating\": 5,\n" +
                        "  \"comment\": \"配送服务很好\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/rating/submit")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(25)
    @DisplayName("电子签收空签名测试")
    void testEmptySignature() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"taskId\": \"task_123456\",\n" +
                        "  \"signatureBase64\": \"\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/signature/submit")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", not(equalTo(0)))
                .body("message", containsString("签名"));
    }

    // ==================== 权限测试 ====================
    @Test
    @Order(26)
    @DisplayName("不同角色配送权限测试")
    void testRolePermissions() {
        Response response = given()
                .header("Authorization", "Bearer " + driverToken)
                .when()
                .get(BASE_PATH + "/drivers/all")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("code", not(equalTo(0)))
                .extract().response();
    }

    @Test
    @Order(27)
    @DisplayName("配送员数据隔离测试")
    void testDataIsolation() {
        Response response = given()
                .header("Authorization", "Bearer " + driverAToken)
                .when()
                .get(BASE_PATH + "/drivers/driver_b_info")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("code", not(equalTo(0)))
                .extract().response();
    }

    // ==================== 异常测试 ====================
    @Test
    @Order(28)
    @DisplayName("未授权访问测试")
    void testUnauthorizedAccess() {
        given()
                .when()
                .get(BASE_PATH + "/tasks")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @Order(29)
    @DisplayName("无效任务ID测试")
    void testInvalidTaskId() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/tasks/invalid_task_999")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", not(equalTo(0)));
    }
}