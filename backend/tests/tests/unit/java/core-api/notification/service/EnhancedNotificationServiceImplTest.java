package cn.aiedge.notification.service;

import cn.aiedge.notification.mapper.NotificationRecordMapper;
import cn.aiedge.notification.mapper.NotificationTemplateMapper;
import cn.aiedge.notification.cache.NotificationTemplateCache;
import cn.aiedge.notification.channel.NotificationChannel;
import cn.aiedge.notification.config.NotificationProperties;
import cn.aiedge.notification.entity.NotificationRecord;
import cn.aiedge.notification.entity.NotificationTemplate;
import cn.aiedge.notification.limiter.NotificationRateLimiter;
import cn.aiedge.notification.service.impl.EnhancedNotificationServiceImpl;
import cn.aiedge.notification.template.TemplateRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 增强通知服务单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("增强通知服务测试")
class EnhancedNotificationServiceImplTest {

    @Mock
    private NotificationTemplateMapper templateMapper;

    @Mock
    private NotificationRecordMapper recordMapper;

    @Mock
    private TemplateRenderer templateRenderer;

    @Mock
    private List<NotificationChannel> channels;

    @Mock
    private NotificationTemplateCache templateCache;

    @Mock
    private NotificationRateLimiter rateLimiter;

    @Mock
    private NotificationProperties properties;

    @Mock
    private ThreadPoolTaskExecutor notificationExecutor;

    @InjectMocks
    private EnhancedNotificationServiceImpl notificationService;

    private NotificationTemplate testTemplate;
    private Map<String, Object> testVariables;

    @BeforeEach
    void setUp() {
        testTemplate = new NotificationTemplate();
        testTemplate.setId(1L);
        testTemplate.setTemplateCode("TEST_TEMPLATE");
        testTemplate.setTemplateName("测试模板");
        testTemplate.setTitle("测试标题: {{name}}");
        testTemplate.setContent("测试内容: {{message}}");
        testTemplate.setNotifyType("sms");
        testTemplate.setStatus(NotificationTemplate.STATUS_ENABLED);
        testTemplate.setCreateTime(LocalDateTime.now());
        testTemplate.setUpdateTime(LocalDateTime.now());

        testVariables = new HashMap<>();
        testVariables.put("name", "张三");
        testVariables.put("message", "欢迎使用");
    }

    // ==================== 模板管理 ====================

    @Test
    @DisplayName("创建模板 - 成功")
    void testCreateTemplate_Success() {
        // Given
        when(templateMapper.insert(any(NotificationTemplate.class))).thenReturn(1);

        // When
        NotificationTemplate result = notificationService.createTemplate(testTemplate);

        // Then
        assertNotNull(result);
        assertEquals(NotificationTemplate.STATUS_ENABLED, result.getStatus());
        verify(templateCache).updateTemplate(any(NotificationTemplate.class));
    }

    @Test
    @DisplayName("创建模板 - 指定状态")
    void testCreateTemplate_WithStatus() {
        // Given
        testTemplate.setStatus(NotificationTemplate.STATUS_DISABLED);
        when(templateMapper.insert(any(NotificationTemplate.class))).thenReturn(1);

        // When
        NotificationTemplate result = notificationService.createTemplate(testTemplate);

        // Then
        assertNotNull(result);
        assertEquals(NotificationTemplate.STATUS_DISABLED, result.getStatus());
    }

    @Test
    @DisplayName("更新模板 - 成功")
    void testUpdateTemplate_Success() {
        // Given
        testTemplate.setTemplateName("更新后的名称");
        when(templateMapper.updateById(any(NotificationTemplate.class))).thenReturn(1);

        // When
        NotificationTemplate result = notificationService.updateTemplate(testTemplate);

        // Then
        assertNotNull(result);
        assertEquals("更新后的名称", result.getTemplateName());
        verify(templateCache).updateTemplate(any(NotificationTemplate.class));
    }

    @Test
    @DisplayName("删除模板 - 成功")
    void testDeleteTemplate_Success() {
        // Given
        when(templateMapper.deleteById(anyLong())).thenReturn(1);

        // When
        boolean result = notificationService.deleteTemplate(1L);

        // Then
        assertTrue(result);
        verify(templateCache).removeTemplate(1L);
    }

    @Test
    @DisplayName("获取模板 - 通过ID")
    void testGetTemplate_ById() {
        // Given
        when(templateCache.getById(anyLong())).thenReturn(testTemplate);

        // When
        NotificationTemplate result = notificationService.getTemplate(1L);

        // Then
        assertNotNull(result);
        assertEquals("TEST_TEMPLATE", result.getTemplateCode());
    }

    @Test
    @DisplayName("获取模板 - 通过编码")
    void testGetTemplateByCode_Success() {
        // Given
        when(templateCache.getByCode(anyString())).thenReturn(testTemplate);

        // When
        NotificationTemplate result = notificationService.getTemplateByCode("TEST_TEMPLATE");

        // Then
        assertNotNull(result);
        assertEquals("测试模板", result.getTemplateName());
    }

    @Test
    @DisplayName("获取所有模板 - 成功")
    void testGetAllTemplates_Success() {
        // Given
        NotificationTemplate template2 = new NotificationTemplate();
        template2.setId(2L);
        template2.setTemplateCode("TEMPLATE_2");
        template2.setStatus(NotificationTemplate.STATUS_ENABLED);
        
        when(templateCache.getAllEnabled()).thenReturn(Arrays.asList(testTemplate, template2));

        // When
        List<NotificationTemplate> result = notificationService.getAllTemplates();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("按类型获取模板 - 成功")
    void testGetTemplatesByType_Success() {
        // Given
        testTemplate.setNotifyType("sms");
        NotificationTemplate emailTemplate = new NotificationTemplate();
        emailTemplate.setId(2L);
        emailTemplate.setNotifyType("email");
        emailTemplate.setStatus(NotificationTemplate.STATUS_ENABLED);
        
        when(templateCache.getAllEnabled()).thenReturn(Arrays.asList(testTemplate, emailTemplate));

        // When
        List<NotificationTemplate> result = notificationService.getTemplatesByType("sms");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("sms", result.get(0).getNotifyType());
    }

    // ==================== 通知发送 ====================

    @Test
    @DisplayName("增强发送 - 使用模板")
    void testSendEnhanced_WithTemplate() {
        // Given
        when(templateCache.getByCode(anyString())).thenReturn(testTemplate);
        when(templateRenderer.render(anyString(), anyMap())).thenReturn("渲染后的内容");
        when(recordMapper.insert(any(NotificationRecord.class))).thenReturn(1);
        when(recordMapper.updateById(any(NotificationRecord.class))).thenReturn(1);
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(properties.isAsyncEnabled()).thenReturn(false);

        NotificationSendRequest request = NotificationSendRequest.builder()
            .templateCode("TEST_TEMPLATE")
            .recipient("13800138000")
            .variables(testVariables)
            .async(false)
            .build();

        // When
        NotificationRecord result = notificationService.sendEnhanced(request);

        // Then
        assertNotNull(result);
        assertEquals("TEST_TEMPLATE", result.getTemplateCode());
        assertNotNull(result.getTitle());
        assertNotNull(result.getContent());
    }

    @Test
    @DisplayName("增强发送 - 模板不存在")
    void testSendEnhanced_TemplateNotFound() {
        // Given
        when(templateCache.getByCode(anyString())).thenReturn(null);

        NotificationSendRequest request = NotificationSendRequest.builder()
            .templateCode("NON_EXISTENT")
            .recipient("13800138000")
            .build();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            notificationService.sendEnhanced(request);
        });
        assertEquals("模板不存在: NON_EXISTENT", exception.getMessage());
    }

    @Test
    @DisplayName("增强发送 - 直接发送")
    void testSendEnhanced_Direct() {
        // Given
        when(recordMapper.insert(any(NotificationRecord.class))).thenReturn(1);
        when(recordMapper.updateById(any(NotificationRecord.class))).thenReturn(1);
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(properties.isAsyncEnabled()).thenReturn(false);

        NotificationSendRequest request = NotificationSendRequest.builder()
            .templateCode("__direct_sms")
            .recipient("13800138000")
            .title("直接标题")
            .content("直接内容")
            .async(false)
            .build();

        // When
        NotificationRecord result = notificationService.sendEnhanced(request);

        // Then
        assertNotNull(result);
        assertEquals("直接标题", result.getTitle());
        assertEquals("直接内容", result.getContent());
    }

    @Test
    @DisplayName("增强发送 - 定时发送")
    void testSendEnhanced_Scheduled() {
        // Given
        when(templateCache.getByCode(anyString())).thenReturn(testTemplate);
        when(templateRenderer.render(anyString(), anyMap())).thenReturn("渲染后的内容");
        doAnswer(i -> {
            NotificationRecord record = i.getArgument(0);
            record.setId(1L);
            return 1;
        }).when(recordMapper).insert(any(NotificationRecord.class));
        when(recordMapper.updateById(any(NotificationRecord.class))).thenReturn(1);

        NotificationSendRequest request = NotificationSendRequest.builder()
            .templateCode("TEST_TEMPLATE")
            .recipient("13800138000")
            .variables(testVariables)
            .scheduledTime(LocalDateTime.now().plusHours(1))
            .priority(5)
            .build();

        // When
        NotificationRecord result = notificationService.sendEnhanced(request);

        // Then
        assertNotNull(result);
        assertEquals(NotificationRecord.STATUS_PENDING, result.getStatus());
        assertNotNull(result.getScheduledTime());
    }

    @Test
    @DisplayName("增强发送 - 限流触发")
    void testSendEnhanced_RateLimited() {
        // Given
        when(templateCache.getByCode(anyString())).thenReturn(testTemplate);
        when(templateRenderer.render(anyString(), anyMap())).thenReturn("渲染后的内容");
        when(recordMapper.insert(any(NotificationRecord.class))).thenReturn(1);
        when(recordMapper.updateById(any(NotificationRecord.class))).thenReturn(1);
        when(rateLimiter.tryAcquire(anyString())).thenReturn(false);

        NotificationSendRequest request = NotificationSendRequest.builder()
            .templateCode("TEST_TEMPLATE")
            .recipient("13800138000")
            .variables(testVariables)
            .build();

        // When
        NotificationRecord result = notificationService.sendEnhanced(request);

        // Then
        assertNotNull(result);
        assertEquals(NotificationRecord.STATUS_PENDING, result.getStatus());
    }

    @Test
    @DisplayName("批量发送 - 成功")
    void testSendBatch_Success() {
        // Given
        when(templateCache.getByCode(anyString())).thenReturn(testTemplate);
        when(templateRenderer.render(anyString(), anyMap())).thenReturn("渲染后的内容");
        doAnswer(i -> {
            NotificationRecord record = i.getArgument(0);
            record.setId(1L);
            return 1;
        }).when(recordMapper).insert(any(NotificationRecord.class));
        when(recordMapper.updateById(any(NotificationRecord.class))).thenReturn(1);
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(properties.isAsyncEnabled()).thenReturn(false);

        List<String> recipients = Arrays.asList("13800138001", "13800138002", "13800138003");
        NotificationSendRequest request = NotificationSendRequest.builder()
            .templateCode("TEST_TEMPLATE")
            .variables(testVariables)
            .build();

        // When
        List<NotificationRecord> results = notificationService.sendBatch(recipients, request);

        // Then
        assertNotNull(results);
        assertEquals(3, results.size());
    }

    // ==================== Helper Methods ====================
}
"@