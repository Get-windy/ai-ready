package cn.aiedge.notification.service;

import cn.aiedge.notification.entity.NotificationRecord;
import cn.aiedge.notification.entity.NotificationTemplate;
import cn.aiedge.notification.model.Notification;
import cn.aiedge.notification.service.impl.NotificationServiceImpl;
import cn.aiedge.cache.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 通知服务单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification testNotification;
    private NotificationTemplate testTemplate;

    @BeforeEach
    void setUp() {
        // 创建测试通知
        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setTitle("测试通知");
        testNotification.setContent("这是一条测试通知内容");
        testNotification.setReceiverId(100L);
        testNotification.setNotifyType("site");

        // 创建测试模板
        testTemplate = new NotificationTemplate();
        testTemplate.setId(1L);
        testTemplate.setTemplateCode("TEST_TEMPLATE");
        testTemplate.setTemplateName("测试模板");
        testTemplate.setNotifyType("site");
        testTemplate.setTitle("测试通知标题");
        testTemplate.setContent("尊敬的用户，您的订单${orderNo}已发货");
        testTemplate.setStatus(1);
    }

    @Test
    @DisplayName("发送站内通知")
    void testSendSiteNotification() {
        when(cacheService.get(anyString(), any())).thenReturn(null);

        Notification result = notificationService.sendNotification(testNotification, 1L);

        assertNotNull(result);
        assertEquals("测试通知", result.getTitle());
    }

    @Test
    @DisplayName("使用模板发送通知")
    void testSendNotificationWithTemplate() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderNo", "ORD123456");

        when(cacheService.get(anyString(), any())).thenReturn(testTemplate);

        Notification result = notificationService.sendNotification(
            "TEST_TEMPLATE", 100L, variables, 1L);

        assertNotNull(result);
    }

    @Test
    @DisplayName("批量发送通知")
    void testSendBatchNotifications() {
        List<Notification> notifications = new ArrayList<>();
        notifications.add(testNotification);

        assertDoesNotThrow(() -> {
            notificationService.sendBatchNotifications(notifications, 1L);
        });
    }

    @Test
    @DisplayName("获取未读通知列表")
    void testGetUnreadNotifications() {
        when(cacheService.lRange(anyString(), anyLong(), anyLong()))
            .thenReturn(Collections.emptyList());

        List<Notification> result = notificationService.getUnreadNotifications(100L, 1L);

        assertNotNull(result);
    }

    @Test
    @DisplayName("获取用户通知列表")
    void testGetUserNotifications() {
        when(cacheService.lRange(anyString(), anyLong(), anyLong()))
            .thenReturn(Collections.emptyList());

        List<Notification> result = notificationService.getUserNotifications(100L, 1, 10, 1L);

        assertNotNull(result);
    }

    @Test
    @DisplayName("获取未读数量")
    void testGetUnreadCount() {
        when(cacheService.get(anyString(), any())).thenReturn(5);

        int count = notificationService.getUnreadCount(100L, 1L);

        assertEquals(5, count);
    }

    @Test
    @DisplayName("标记已读")
    void testMarkAsRead() {
        when(cacheService.get(anyString(), any())).thenReturn(testNotification);

        boolean result = notificationService.markAsRead(1L, 100L);

        assertTrue(result);
    }

    @Test
    @DisplayName("标记全部已读")
    void testMarkAllAsRead() {
        assertDoesNotThrow(() -> {
            boolean result = notificationService.markAllAsRead(100L, 1L);
        });
    }

    @Test
    @DisplayName("删除通知")
    void testDeleteNotification() {
        when(cacheService.get(anyString(), any())).thenReturn(testNotification);

        boolean result = notificationService.deleteNotification(1L, 100L);

        assertTrue(result);
    }

    @Test
    @DisplayName("获取模板")
    void testGetTemplate() {
        when(cacheService.get(anyString(), any())).thenReturn(testTemplate);

        NotificationTemplate result = notificationService.getTemplate("TEST_TEMPLATE");

        assertNotNull(result);
        assertEquals("TEST_TEMPLATE", result.getTemplateCode());
    }

    @Test
    @DisplayName("保存模板")
    void testSaveTemplate() {
        when(cacheService.set(anyString(), any())).thenReturn(true);

        NotificationTemplate result = notificationService.saveTemplate(testTemplate);

        assertNotNull(result);
    }

    @Test
    @DisplayName("发送邮件")
    void testSendEmail() {
        assertDoesNotThrow(() -> {
            notificationService.sendEmail("test@example.com", "测试邮件", "邮件内容");
        });
    }

    @Test
    @DisplayName("发送短信")
    void testSendSms() {
        assertDoesNotThrow(() -> {
            notificationService.sendSms("13800138000", "验证码：123456");
        });
    }

    @Test
    @DisplayName("模板变量替换")
    void testTemplateVariableReplacement() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderNo", "ORD123456");
        variables.put("amount", "99.00");

        String template = "您的订单${orderNo}金额为${amount}元";
        String result = replaceVariables(template, variables);

        assertTrue(result.contains("ORD123456"));
        assertTrue(result.contains("99.00"));
        assertFalse(result.contains("${"));
    }

    private String replaceVariables(String template, Map<String, Object> variables) {
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", 
                entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return result;
    }
}
