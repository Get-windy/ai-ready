package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysMessage;
import cn.aiedge.base.entity.SysMessageTemplate;
import cn.aiedge.base.mapper.SysMessageMapper;
import cn.aiedge.base.mapper.SysMessageTemplateMapper;
import cn.aiedge.base.service.impl.MessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 消息通知服务单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private SysMessageMapper messageMapper;

    @Mock
    private SysMessageTemplateMapper templateMapper;

    @InjectMocks
    private MessageServiceImpl messageService;

    private SysMessage testMessage;
    private SysMessageTemplate testTemplate;

    @BeforeEach
    void setUp() {
        testMessage = new SysMessage();
        testMessage.setId(1L);
        testMessage.setMsgType(2);
        testMessage.setTitle("测试消息");
        testMessage.setContent("测试内容");
        testMessage.setReceiverId(1L);
        testMessage.setSendStatus(2);

        testTemplate = new SysMessageTemplate();
        testTemplate.setId(1L);
        testTemplate.setTemplateCode("TEST_TEMPLATE");
        testTemplate.setTemplateName("测试模板");
        testTemplate.setTitle("通知：${title}");
        testTemplate.setContent("您好，${name}，您有新的消息：${message}");
        testTemplate.setContentType("text");
    }

    @Test
    void testSendSiteMessage() {
        when(messageMapper.insert(any(SysMessage.class))).thenAnswer(invocation -> {
            SysMessage message = invocation.getArgument(0);
            message.setId(1L);
            return 1;
        });

        Long messageId = messageService.sendSiteMessage(1L, "测试标题", "测试内容", "TEST", 1L);

        assertNotNull(messageId);
        verify(messageMapper).insert(any(SysMessage.class));
    }

    @Test
    void testSendSiteMessageByTemplate() {
        when(templateMapper.selectByCode(anyString(), anyLong())).thenReturn(testTemplate);
        when(messageMapper.insert(any(SysMessage.class))).thenAnswer(invocation -> {
            SysMessage message = invocation.getArgument(0);
            message.setId(1L);
            return 1;
        });

        Map<String, Object> params = new HashMap<>();
        params.put("title", "会议通知");
        params.put("name", "张三");
        params.put("message", "明天下午2点开会");

        Long messageId = messageService.sendSiteMessageByTemplate(1L, "TEST_TEMPLATE", params, "TEST", 1L);

        assertNotNull(messageId);
        verify(templateMapper).selectByCode(eq("TEST_TEMPLATE"), anyLong());
    }

    @Test
    void testSendEmail() {
        when(messageMapper.insert(any(SysMessage.class))).thenAnswer(invocation -> {
            SysMessage message = invocation.getArgument(0);
            message.setId(1L);
            return 1;
        });

        Long messageId = messageService.sendEmail(1L, "test@test.com", "邮件标题", "邮件内容", "TEST", 1L);

        assertNotNull(messageId);
        verify(messageMapper).insert(any(SysMessage.class));
    }

    @Test
    void testRenderTemplate() {
        String template = "您好，${name}，您的订单${orderId}已发货";
        Map<String, Object> params = new HashMap<>();
        params.put("name", "张三");
        params.put("orderId", "12345");

        String result = messageService.renderTemplate(template, params);

        assertEquals("您好，张三，您的订单12345已发货", result);
    }

    @Test
    void testRenderTemplate_NullParams() {
        String template = "您好，${name}";

        String result = messageService.renderTemplate(template, null);

        assertEquals("您好，${name}", result);
    }

    @Test
    void testMarkAsRead() {
        when(messageMapper.markAsRead(anyLong())).thenReturn(1);

        messageService.markAsRead(1L);

        verify(messageMapper).markAsRead(1L);
    }

    @Test
    void testCreateTemplate() {
        when(templateMapper.selectByCode(anyString(), anyLong())).thenReturn(null);
        when(templateMapper.insert(any(SysMessageTemplate.class))).thenReturn(1);

        Long templateId = messageService.createTemplate(testTemplate);

        assertNotNull(templateId);
        verify(templateMapper).insert(any(SysMessageTemplate.class));
    }

    @Test
    void testCreateTemplate_DuplicateCode() {
        when(templateMapper.selectByCode(anyString(), anyLong())).thenReturn(testTemplate);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            messageService.createTemplate(testTemplate);
        });
        assertTrue(exception.getMessage().contains("模板编码已存在"));
    }

    @Test
    void testGetTemplateByCode() {
        when(templateMapper.selectByCode(anyString(), anyLong())).thenReturn(testTemplate);

        SysMessageTemplate result = messageService.getTemplateByCode("TEST_TEMPLATE");

        assertNotNull(result);
        assertEquals("TEST_TEMPLATE", result.getTemplateCode());
    }
}
