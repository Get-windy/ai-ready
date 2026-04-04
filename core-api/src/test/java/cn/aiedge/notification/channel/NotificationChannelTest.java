package cn.aiedge.notification.channel;

import cn.aiedge.notification.channel.EmailChannel;
import cn.aiedge.notification.channel.SmsChannel;
import cn.aiedge.notification.channel.SiteMessageChannel;
import cn.aiedge.notification.entity.NotificationRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 通知渠道单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
class NotificationChannelTest {

    private NotificationRecord testRecord;

    @BeforeEach
    void setUp() {
        testRecord = NotificationRecord.builder()
            .id(1L)
            .notifyType("site")
            .receiverId(100L)
            .title("测试通知")
            .content("测试内容")
            .build();
    }

    @Test
    @DisplayName("站内消息渠道 - 发送成功")
    void testSiteMessageChannel_Send() {
        SiteMessageChannel channel = new SiteMessageChannel();
        
        assertEquals("site", channel.getChannelType());
        
        NotificationChannel.SendResult result = channel.send(testRecord);
        
        assertNotNull(result);
    }

    @Test
    @DisplayName("站内消息渠道 - 可用性检查")
    void testSiteMessageChannel_Available() {
        SiteMessageChannel channel = new SiteMessageChannel();
        
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("邮件渠道 - 渠道类型")
    void testEmailChannel_Type() {
        EmailChannel channel = new EmailChannel();
        
        assertEquals("email", channel.getChannelType());
    }

    @Test
    @DisplayName("邮件渠道 - 发送")
    void testEmailChannel_Send() {
        EmailChannel channel = new EmailChannel();
        
        testRecord.setNotifyType("email");
        testRecord.setReceiverAddress("test@example.com");
        
        NotificationChannel.SendResult result = channel.send(testRecord);
        
        assertNotNull(result);
    }

    @Test
    @DisplayName("短信渠道 - 渠道类型")
    void testSmsChannel_Type() {
        SmsChannel channel = new SmsChannel();
        
        assertEquals("sms", channel.getChannelType());
    }

    @Test
    @DisplayName("短信渠道 - 发送")
    void testSmsChannel_Send() {
        SmsChannel channel = new SmsChannel();
        
        testRecord.setNotifyType("sms");
        testRecord.setReceiverAddress("13800138000");
        
        NotificationChannel.SendResult result = channel.send(testRecord);
        
        assertNotNull(result);
    }

    @Test
    @DisplayName("发送结果 - 成功")
    void testSendResult_Success() {
        NotificationChannel.SendResult result = NotificationChannel.SendResult.success();
        
        assertTrue(result.success());
        assertEquals("发送成功", result.message());
    }

    @Test
    @DisplayName("发送结果 - 成功带外部ID")
    void testSendResult_SuccessWithExternalId() {
        NotificationChannel.SendResult result = NotificationChannel.SendResult.success("MSG123");
        
        assertTrue(result.success());
        assertEquals("MSG123", result.externalId());
    }

    @Test
    @DisplayName("发送结果 - 失败")
    void testSendResult_Failure() {
        NotificationChannel.SendResult result = NotificationChannel.SendResult.failure("发送失败");
        
        assertFalse(result.success());
        assertEquals("发送失败", result.message());
    }

    @Test
    @DisplayName("通知记录构建器测试")
    void testNotificationRecordBuilder() {
        NotificationRecord record = NotificationRecord.builder()
            .templateCode("TEST_TEMPLATE")
            .notifyType("email")
            .receiverId(100L)
            .receiverType("user")
            .receiverAddress("test@example.com")
            .title("测试标题")
            .content("测试内容")
            .status(NotificationRecord.STATUS_PENDING)
            .readStatus(NotificationRecord.READ_UNREAD)
            .retryCount(0)
            .build();
        
        assertEquals("TEST_TEMPLATE", record.getTemplateCode());
        assertEquals("email", record.getNotifyType());
        assertEquals(100L, record.getReceiverId());
        assertEquals("user", record.getReceiverType());
        assertEquals(NotificationRecord.STATUS_PENDING, record.getStatus());
    }

    @Test
    @DisplayName("通知记录状态常量")
    void testNotificationRecordStatusConstants() {
        assertEquals(0, NotificationRecord.STATUS_PENDING);
        assertEquals(1, NotificationRecord.STATUS_SENDING);
        assertEquals(2, NotificationRecord.STATUS_SUCCESS);
        assertEquals(3, NotificationRecord.STATUS_FAILED);
        
        assertEquals(0, NotificationRecord.READ_UNREAD);
        assertEquals(1, NotificationRecord.READ_READ);
        
        assertEquals("user", NotificationRecord.RECEIVER_USER);
        assertEquals("role", NotificationRecord.RECEIVER_ROLE);
        assertEquals("all", NotificationRecord.RECEIVER_ALL);
    }
}
