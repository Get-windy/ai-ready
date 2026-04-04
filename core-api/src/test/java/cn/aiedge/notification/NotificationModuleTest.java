package cn.aiedge.notification;

import cn.aiedge.notification.model.Notification;
import cn.aiedge.notification.model.NotificationTemplate;
import cn.aiedge.notification.service.NotificationService;
import cn.aiedge.notification.channel.EmailChannel;
import cn.aiedge.notification.channel.SmsChannel;
import cn.aiedge.notification.channel.SiteMessageChannel;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 消息通知模块单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificationModuleTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailChannel emailChannel;

    @Mock
    private SmsChannel smsChannel;

    @Mock
    private SiteMessageChannel siteMessageChannel;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    // ==================== 通知实体测试 ====================

    @Test
    @Order(1)
    @DisplayName("通知实体创建测试")
    void testNotificationCreation() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUserId(100L);
        notification.setTitle("测试通知");
        notification.setContent("这是一条测试通知");
        notification.setChannel("in_app");
        notification.setRead(false);
        notification.setCreateTime(LocalDateTime.now());

        assertNotNull(notification);
        assertEquals("测试通知", notification.getTitle());
        assertFalse(notification.getRead());
    }

    @Test
    @Order(2)
    @DisplayName("通知模板创建测试")
    void testNotificationTemplateCreation() {
        NotificationTemplate template = new NotificationTemplate();
        template.setTemplateCode("test_template");
        template.setTemplateName("测试模板");
        template.setChannel("email");
        template.setTitleTemplate("尊敬的${username}：");
        template.setContentTemplate("您有一条新消息：${message}");
        template.setEnabled(true);

        assertNotNull(template);
        assertEquals("test_template", template.getTemplateCode());
        assertTrue(template.getEnabled());
    }

    // ==================== 发送通知测试 ====================

    @Test
    @Order(10)
    @DisplayName("发送单个通知 - 成功")
    void testSendNotificationSuccess() {
        Notification notification = createTestNotification();
        notification.setId(1L);

        when(notificationService.sendNotification(any(Notification.class), anyLong()))
                .thenReturn(notification);

        Notification result = notificationService.sendNotification(notification, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(notificationService, times(1)).sendNotification(any(), anyLong());
    }

    @Test
    @Order(11)
    @DisplayName("使用模板发送通知 - 成功")
    void testSendNotificationWithTemplate() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", "张三");
        variables.put("message", "您的订单已发货");

        Notification notification = createTestNotification();
        notification.setId(1L);

        when(notificationService.sendNotification(anyString(), anyLong(), anyMap(), anyLong()))
                .thenReturn(notification);

        Notification result = notificationService.sendNotification(
                "order_shipped", 100L, variables, 1L);

        assertNotNull(result);
    }

    @Test
    @Order(12)
    @DisplayName("批量发送通知 - 成功")
    void testSendBatchNotificationsSuccess() {
        List<Notification> notifications = Arrays.asList(
                createTestNotification(),
                createTestNotification()
        );

        doNothing().when(notificationService).sendBatchNotifications(anyList(), anyLong());

        notificationService.sendBatchNotifications(notifications, 1L);

        verify(notificationService, times(1)).sendBatchNotifications(anyList(), anyLong());
    }

    // ==================== 获取通知测试 ====================

    @Test
    @Order(20)
    @DisplayName("获取单个通知 - 成功")
    void testGetNotificationSuccess() {
        Notification notification = createTestNotification();
        notification.setId(1L);

        when(notificationService.getNotification(1L)).thenReturn(notification);

        Notification result = notificationService.getNotification(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @Order(21)
    @DisplayName("获取未读通知列表 - 成功")
    void testGetUnreadNotificationsSuccess() {
        List<Notification> notifications = Arrays.asList(
                createTestNotification(),
                createTestNotification()
        );

        when(notificationService.getUnreadNotifications(100L, 1L))
                .thenReturn(notifications);

        List<Notification> result = notificationService.getUnreadNotifications(100L, 1L);

        assertEquals(2, result.size());
    }

    @Test
    @Order(22)
    @DisplayName("分页获取用户通知 - 成功")
    void testGetUserNotificationsWithPaging() {
        List<Notification> notifications = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Notification n = createTestNotification();
            n.setId((long) i);
            notifications.add(n);
        }

        when(notificationService.getUserNotifications(100L, 1, 10, 1L))
                .thenReturn(notifications);

        List<Notification> result = notificationService.getUserNotifications(100L, 1, 10, 1L);

        assertEquals(10, result.size());
    }

    @Test
    @Order(23)
    @DisplayName("获取未读通知数量 - 成功")
    void testGetUnreadCountSuccess() {
        when(notificationService.getUnreadCount(100L, 1L)).thenReturn(5);

        int count = notificationService.getUnreadCount(100L, 1L);

        assertEquals(5, count);
    }

    // ==================== 标记已读测试 ====================

    @Test
    @Order(30)
    @DisplayName("标记单条已读 - 成功")
    void testMarkAsReadSuccess() {
        when(notificationService.markAsRead(1L, 100L)).thenReturn(true);

        boolean result = notificationService.markAsRead(1L, 100L);

        assertTrue(result);
    }

    @Test
    @Order(31)
    @DisplayName("标记全部已读 - 成功")
    void testMarkAllAsReadSuccess() {
        when(notificationService.markAllAsRead(100L, 1L)).thenReturn(true);

        boolean result = notificationService.markAllAsRead(100L, 1L);

        assertTrue(result);
    }

    // ==================== 删除通知测试 ====================

    @Test
    @Order(40)
    @DisplayName("删除单条通知 - 成功")
    void testDeleteNotificationSuccess() {
        when(notificationService.deleteNotification(1L, 100L)).thenReturn(true);

        boolean result = notificationService.deleteNotification(1L, 100L);

        assertTrue(result);
    }

    @Test
    @Order(41)
    @DisplayName("删除所有已读通知 - 成功")
    void testDeleteAllReadSuccess() {
        when(notificationService.deleteAllRead(100L, 1L)).thenReturn(true);

        boolean result = notificationService.deleteAllRead(100L, 1L);

        assertTrue(result);
    }

    // ==================== 模板管理测试 ====================

    @Test
    @Order(50)
    @DisplayName("获取模板 - 成功")
    void testGetTemplateSuccess() {
        NotificationTemplate template = createTestTemplate();

        when(notificationService.getTemplate("test_template")).thenReturn(template);

        NotificationTemplate result = notificationService.getTemplate("test_template");

        assertNotNull(result);
        assertEquals("test_template", result.getTemplateCode());
    }

    @Test
    @Order(51)
    @DisplayName("获取渠道模板列表 - 成功")
    void testGetTemplatesByChannel() {
        List<NotificationTemplate> templates = Arrays.asList(
                createTestTemplate(),
                createTestTemplate()
        );

        when(notificationService.getTemplates("email", 1L)).thenReturn(templates);

        List<NotificationTemplate> result = notificationService.getTemplates("email", 1L);

        assertEquals(2, result.size());
    }

    @Test
    @Order(52)
    @DisplayName("保存模板 - 成功")
    void testSaveTemplateSuccess() {
        NotificationTemplate template = createTestTemplate();
        template.setId(1L);

        when(notificationService.saveTemplate(any(NotificationTemplate.class)))
                .thenReturn(template);

        NotificationTemplate result = notificationService.saveTemplate(template);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    // ==================== 多渠道推送测试 ====================

    @Test
    @Order(60)
    @DisplayName("发送邮件 - 成功")
    void testSendEmailSuccess() {
        doNothing().when(notificationService).sendEmail(anyString(), anyString(), anyString());

        notificationService.sendEmail("test@example.com", "测试主题", "测试内容");

        verify(notificationService, times(1)).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    @Order(61)
    @DisplayName("发送短信 - 成功")
    void testSendSmsSuccess() {
        doNothing().when(notificationService).sendSms(anyString(), anyString());

        notificationService.sendSms("13800138000", "您的验证码是123456");

        verify(notificationService, times(1)).sendSms(anyString(), anyString());
    }

    @Test
    @Order(62)
    @DisplayName("发送微信消息 - 成功")
    void testSendWechatSuccess() {
        doNothing().when(notificationService).sendWechat(anyString(), anyString());

        notificationService.sendWechat("openid123", "您有新消息");

        verify(notificationService, times(1)).sendWechat(anyString(), anyString());
    }

    @Test
    @Order(63)
    @DisplayName("推送给指定用户 - 成功")
    void testPushToUserSuccess() {
        Notification notification = createTestNotification();

        doNothing().when(notificationService).pushToUser(anyLong(), anyString(), any(Notification.class));

        notificationService.pushToUser(100L, "in_app", notification);

        verify(notificationService, times(1)).pushToUser(anyLong(), anyString(), any());
    }

    // ==================== 渠道测试 ====================

    @Test
    @Order(70)
    @DisplayName("邮件渠道发送 - 成功")
    void testEmailChannelSend() {
        doNothing().when(emailChannel).send(anyString(), anyString(), anyString());

        emailChannel.send("test@example.com", "主题", "内容");

        verify(emailChannel, times(1)).send(anyString(), anyString(), anyString());
    }

    @Test
    @Order(71)
    @DisplayName("短信渠道发送 - 成功")
    void testSmsChannelSend() {
        doNothing().when(smsChannel).send(anyString(), anyString());

        smsChannel.send("13800138000", "验证码：123456");

        verify(smsChannel, times(1)).send(anyString(), anyString());
    }

    @Test
    @Order(72)
    @DisplayName("站内信渠道发送 - 成功")
    void testSiteMessageChannelSend() {
        doNothing().when(siteMessageChannel).send(anyLong(), anyString(), anyString());

        siteMessageChannel.send(100L, "标题", "内容");

        verify(siteMessageChannel, times(1)).send(anyLong(), anyString(), anyString());
    }

    // ==================== 边界条件测试 ====================

    @Test
    @Order(80)
    @DisplayName("获取不存在的通知 - 返回null")
    void testGetNonExistentNotification() {
        when(notificationService.getNotification(999L)).thenReturn(null);

        Notification result = notificationService.getNotification(999L);

        assertNull(result);
    }

    @Test
    @Order(81)
    @DisplayName("获取空未读列表 - 返回空列表")
    void testGetEmptyUnreadNotifications() {
        when(notificationService.getUnreadNotifications(100L, 1L))
                .thenReturn(Collections.emptyList());

        List<Notification> result = notificationService.getUnreadNotifications(100L, 1L);

        assertTrue(result.isEmpty());
    }

    @Test
    @Order(82)
    @DisplayName("未读数量为0")
    void testZeroUnreadCount() {
        when(notificationService.getUnreadCount(100L, 1L)).thenReturn(0);

        int count = notificationService.getUnreadCount(100L, 1L);

        assertEquals(0, count);
    }

    // ==================== 辅助方法 ====================

    private Notification createTestNotification() {
        Notification notification = new Notification();
        notification.setUserId(100L);
        notification.setTitle("测试通知");
        notification.setContent("这是一条测试通知内容");
        notification.setChannel("in_app");
        notification.setRead(false);
        notification.setCreateTime(LocalDateTime.now());
        return notification;
    }

    private NotificationTemplate createTestTemplate() {
        NotificationTemplate template = new NotificationTemplate();
        template.setTemplateCode("test_template");
        template.setTemplateName("测试模板");
        template.setChannel("email");
        template.setTitleTemplate("尊敬的用户：");
        template.setContentTemplate("您有一条新消息");
        template.setEnabled(true);
        return template;
    }
}
