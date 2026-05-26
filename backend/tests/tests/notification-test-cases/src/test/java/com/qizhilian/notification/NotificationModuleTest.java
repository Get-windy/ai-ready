package com.qizhilian.notification;

import com.qizhilian.api.base.ApiBaseTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 消息通知模块API测试
 * 
 * 测试覆盖：
 * - 系统通知测试（公告发布/接收/系统升级/维护/紧急通知）
 * - 审批提醒测试（待审批/结果/超时/转交通知）
 * - 业务提醒测试（订单状态/库存预警/账款到期/任务截止）
 * - 消息推送测试（站内信/邮件/短信/企业微信）
 * - 消息管理测试（列表查询/已读/删除/归档/搜索）
 * - 权限测试（查看权限/发送权限）
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NotificationModuleTest extends ApiBaseTest {

    private static final String BASE_PATH = "/api/v1/notification";

    // ==================== 系统通知测试 ====================
    @Test
    @Order(1)
    @DisplayName("公告发布测试")
    void testAnnouncementPublish() {
        Response response = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"title\": \"系统升级公告\",\n" +
                        "  \"content\": \"系统将于今晚22:00进行升级\",\n" +
                        "  \"scope\": \"all\",\n" +
                        "  \"priority\": \"high\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/announcement/publish")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.status", equalTo("published"))
                .extract().response();

        String announcementId = response.jsonPath().getString("data.id");
        Assertions.assertNotNull(announcementId, "公告ID不应为空");
    }

    @Test
    @Order(2)
    @DisplayName("公告接收测试")
    void testAnnouncementReceive() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/announcements")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.content", notNullValue())
                .extract().response();
    }

    @Test
    @Order(3)
    @DisplayName("系统升级通知测试")
    void testSystemUpgradeNotification() {
        Response response = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"title\": \"系统升级通知\",\n" +
                        "  \"content\": \"系统将于2026-04-15 22:00升级\",\n" +
                        "  \"upgradeTime\": \"2026-04-15T22:00:00\",\n" +
                        "  \"affectedModules\": [\"订单管理\", \"库存管理\"],\n" +
                        "  \"scope\": \"all\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/system/upgrade")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(4)
    @DisplayName("维护通知测试")
    void testMaintenanceNotification() {
        Response response = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"title\": \"系统维护通知\",\n" +
                        "  \"content\": \"系统将于2026-04-14 02:00-04:00维护\",\n" +
                        "  \"startTime\": \"2026-04-14T02:00:00\",\n" +
                        "  \"endTime\": \"2026-04-14T04:00:00\",\n" +
                        "  \"scope\": \"all\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/system/maintenance")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(5)
    @DisplayName("紧急通知测试")
    void testUrgentNotification() {
        Response response = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"title\": \"紧急通知\",\n" +
                        "  \"content\": \"系统出现异常，请立即查看\",\n" +
                        "  \"pushMethods\": [\"站内信\", \"短信\", \"邮件\"],\n" +
                        "  \"scope\": \"all\",\n" +
                        "  \"immediate\": true\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/system/urgent")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    // ==================== 审批提醒测试 ====================
    @Test
    @Order(6)
    @DisplayName("待审批提醒测试")
    void testPendingApprovalReminder() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"approvalId\": \"approval_123456\",\n" +
                        "  \"reminderType\": \"pending\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/approval/remind")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(7)
    @DisplayName("审批结果通知测试")
    void testApprovalResultNotification() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"approvalId\": \"approval_123456\",\n" +
                        "  \"result\": \"approved\",\n" +
                        "  \"comment\": \"同意申请\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/approval/result")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(8)
    @DisplayName("审批超时提醒测试")
    void testApprovalTimeoutReminder() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("timeoutHours", 24)
                .when()
                .get(BASE_PATH + "/approval/timeout")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.timeoutItems", notNullValue())
                .extract().response();
    }

    @Test
    @Order(9)
    @DisplayName("审批转交通知测试")
    void testApprovalTransferNotification() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"approvalId\": \"approval_123456\",\n" +
                        "  \"fromUser\": \"user_a\",\n" +
                        "  \"toUser\": \"user_b\",\n" +
                        "  \"reason\": \"转交给您处理\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/approval/transfer")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    // ==================== 业务提醒测试 ====================
    @Test
    @Order(10)
    @DisplayName("订单状态变更通知测试")
    void testOrderStatusNotification() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"orderId\": \"order_123456\",\n" +
                        "  \"oldStatus\": \"pending\",\n" +
                        "  \"newStatus\": \"shipped\",\n" +
                        "  \"notifyCustomer\": true\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/business/order/status")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(11)
    @DisplayName("库存预警通知测试")
    void testInventoryWarningNotification() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"productId\": \"prod_123456\",\n" +
                        "  \"currentStock\": 10,\n" +
                        "  \"threshold\": 20,\n" +
                        "  \"notifyUsers\": [\"admin\", \"warehouse\"]\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/business/inventory/warning")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(12)
    @DisplayName("账款到期提醒测试")
    void testPaymentDueNotification() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"billId\": \"bill_123456\",\n" +
                        "  \"dueDate\": \"2026-04-20\",\n" +
                        "  \"amount\": 10000.00,\n" +
                        "  \"notifyDaysBefore\": 3\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/business/payment/due")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(13)
    @DisplayName("任务截止提醒测试")
    void testTaskDeadlineNotification() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"taskId\": \"task_123456\",\n" +
                        "  \"deadline\": \"2026-04-15T18:00:00\",\n" +
                        "  \"notifyHoursBefore\": 24\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/business/task/deadline")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    // ==================== 消息推送测试 ====================
    @Test
    @Order(14)
    @DisplayName("站内信推送测试")
    void testInternalMessagePush() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"receiverIds\": [\"user_123\", \"user_456\"],\n" +
                        "  \"title\": \"站内信通知\",\n" +
                        "  \"content\": \"您有新的消息\",\n" +
                        "  \"type\": \"internal\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/push/internal")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(15)
    @DisplayName("邮件推送测试")
    void testEmailPush() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"to\": [\"test@example.com\"],\n" +
                        "  \"subject\": \"邮件通知\",\n" +
                        "  \"content\": \"这是一封测试邮件\",\n" +
                        "  \"type\": \"email\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/push/email")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(16)
    @DisplayName("短信推送测试")
    void testSmsPush() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"phones\": [\"13800138000\"],\n" +
                        "  \"content\": \"您的验证码是123456\",\n" +
                        "  \"type\": \"sms\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/push/sms")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(17)
    @DisplayName("企业微信推送测试")
    void testWecomPush() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"userIds\": [\"wecom_user_123\"],\n" +
                        "  \"content\": \"企业微信通知测试\",\n" +
                        "  \"type\": \"wecom\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/push/wecom")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(18)
    @DisplayName("推送失败重试测试")
    void testPushRetry() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"messageId\": \"msg_failed_123\",\n" +
                        "  \"retry\": true\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/push/retry")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    // ==================== 消息管理测试 ====================
    @Test
    @Order(19)
    @DisplayName("消息列表查询测试")
    void testMessageListQuery() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("page", 0)
                .param("size", 20)
                .when()
                .get(BASE_PATH + "/messages")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.content", notNullValue())
                .extract().response();
    }

    @Test
    @Order(20)
    @DisplayName("消息已读/未读测试")
    void testMessageReadStatus() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"messageIds\": [\"msg_123456\"],\n" +
                        "  \"read\": true\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/messages/read")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(21)
    @DisplayName("消息删除测试")
    void testMessageDelete() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"messageIds\": [\"msg_123456\"]\n" +
                        "}")
                .when()
                .delete(BASE_PATH + "/messages")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(22)
    @DisplayName("消息归档测试")
    void testMessageArchive() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"messageIds\": [\"msg_123456\"],\n" +
                        "  \"action\": \"archive\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/messages/archive")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .extract().response();
    }

    @Test
    @Order(23)
    @DisplayName("消息搜索测试")
    void testMessageSearch() {
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .param("keyword", "测试")
                .when()
                .get(BASE_PATH + "/messages/search")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo(0))
                .body("data.results", notNullValue())
                .extract().response();
    }

    // ==================== 权限测试 ====================
    @Test
    @Order(24)
    @DisplayName("消息查看权限测试")
    void testMessageViewPermission() {
        Response response = given()
                .header("Authorization", "Bearer " + normalUserToken)
                .when()
                .get(BASE_PATH + "/messages/admin_messages")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("code", not(equalTo(0)))
                .extract().response();
    }

    @Test
    @Order(25)
    @DisplayName("消息发送权限测试")
    void testMessageSendPermission() {
        Response response = given()
                .header("Authorization", "Bearer " + normalUserToken)
                .contentType("application/json")
                .body("{\n" +
                        "  \"title\": \"系统公告\",\n" +
                        "  \"content\": \"测试公告\",\n" +
                        "  \"scope\": \"all\"\n" +
                        "}")
                .when()
                .post(BASE_PATH + "/announcement/publish")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("code", not(equalTo(0)))
                .extract().response();
    }

    // ==================== 异常测试 ====================
    @Test
    @Order(26)
    @DisplayName("未授权访问测试")
    void testUnauthorizedAccess() {
        given()
                .when()
                .get(BASE_PATH + "/messages")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @Order(27)
    @DisplayName("无效消息ID测试")
    void testInvalidMessageId() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(BASE_PATH + "/messages/invalid_msg_999")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", not(equalTo(0)));
    }
}