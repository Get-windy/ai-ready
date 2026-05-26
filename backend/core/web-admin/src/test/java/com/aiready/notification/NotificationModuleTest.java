package com.aiready.notification;

import com.aiready.notification.dto.NotificationTemplateDTO;
import com.aiready.notification.entity.Notification;
import com.aiready.notification.entity.NotificationTemplate;
import com.aiready.notification.service.NotificationService;
import com.aiready.notification.service.NotificationTemplateService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class NotificationModuleTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationTemplateService templateService;

    @Test
    public void testNotificationCRUD() {
        Notification notification = new Notification();
        notification.setTitle("测试通知");
        notification.setContent("这是一条测试通知");
        notification.setType(1);
        notification.setChannel(1);
        notification.setUserId(1L);
        notification.setSenderId(0L);
        notification.setSenderName("系统");
        notification.setReadStatus(0);
        notification.setSendStatus(2);
        
        boolean saveResult = notificationService.save(notification);
        assertTrue(saveResult);
        assertNotNull(notification.getId());
        
        Notification saved = notificationService.getById(notification.getId());
        assertNotNull(saved);
        assertEquals("测试通知", saved.getTitle());
        
        saved.setTitle("更新后的标题");
        boolean updateResult = notificationService.updateById(saved);
        assertTrue(updateResult);
        
        Notification updated = notificationService.getById(notification.getId());
        assertEquals("更新后的标题", updated.getTitle());
        
        boolean deleteResult = notificationService.removeById(notification.getId());
        assertTrue(deleteResult);
        
        Notification deleted = notificationService.getById(notification.getId());
        assertNull(deleted);
    }

    @Test
    public void testTemplateCRUD() {
        NotificationTemplate template = new NotificationTemplate();
        template.setTemplateCode("TEST_TEMPLATE");
        template.setTemplateName("测试模板");
        template.setType(1);
        template.setTitle("测试标题${name}");
        template.setContent("测试内容${content}");
        template.setStatus(1);
        
        boolean saveResult = templateService.save(template);
        assertTrue(saveResult);
        assertNotNull(template.getId());
        
        NotificationTemplate saved = templateService.getByCode("TEST_TEMPLATE");
        assertNotNull(saved);
        assertEquals("测试模板", saved.getTemplateName());
        
        templateService.toggleStatus(saved.getId(), 0);
        NotificationTemplate updated = templateService.getById(saved.getId());
        assertEquals(0, updated.getStatus());
        
        boolean deleteResult = templateService.removeById(saved.getId());
        assertTrue(deleteResult);
    }

    @Test
    public void testTemplateRender() {
        String template = "您好${username}，您的订单${orderNo}已创建成功";
        Map<String, Object> params = new HashMap<>();
        params.put("username", "张三");
        params.put("orderNo", "ORD202404110001");
        
        String result = templateService.renderTemplate(template, params);
        assertEquals("您好张三，您的订单ORD202404110001已创建成功", result);
    }

    @Test
    public void testUnreadCount() {
        Long initialCount = notificationService.getUnreadCount(1L);
        
        Notification notification = new Notification();
        notification.setTitle("未读通知");
        notification.setContent("这是一条未读通知");
        notification.setType(1);
        notification.setChannel(1);
        notification.setUserId(1L);
        notification.setReadStatus(0);
        notification.setSendStatus(2);
        notificationService.save(notification);
        
        Long newCount = notificationService.getUnreadCount(1L);
        assertEquals(initialCount + 1, newCount);
        
        notificationService.markAsRead(notification.getId());
        Long afterRead = notificationService.getUnreadCount(1L);
        assertEquals(initialCount, afterRead);
    }

    @Test
    public void testMarkAllAsRead() {
        for (int i = 0; i < 5; i++) {
            Notification notification = new Notification();
            notification.setTitle("通知" + i);
            notification.setContent("内容" + i);
            notification.setType(1);
            notification.setChannel(1);
            notification.setUserId(2L);
            notification.setReadStatus(0);
            notification.setSendStatus(2);
            notificationService.save(notification);
        }
        
        Long unreadCount = notificationService.getUnreadCount(2L);
        assertTrue(unreadCount >= 5);
        
        notificationService.markAllAsRead(2L);
        
        Long afterMarkAll = notificationService.getUnreadCount(2L);
        assertEquals(0, afterMarkAll);
    }

    @Test
    public void testBatchOperations() {
        Notification n1 = new Notification();
        n1.setTitle("通知1");
        n1.setContent("内容1");
        n1.setUserId(3L);
        n1.setReadStatus(0);
        n1.setSendStatus(2);
        notificationService.save(n1);
        
        Notification n2 = new Notification();
        n2.setTitle("通知2");
        n2.setContent("内容2");
        n2.setUserId(3L);
        n2.setReadStatus(0);
        n2.setSendStatus(2);
        notificationService.save(n2);
        
        List<Long> ids = Arrays.asList(n1.getId(), n2.getId());
        notificationService.markAsReadBatch(ids);
        
        Notification updated1 = notificationService.getById(n1.getId());
        Notification updated2 = notificationService.getById(n2.getId());
        assertEquals(1, updated1.getReadStatus());
        assertEquals(1, updated2.getReadStatus());
        
        notificationService.deleteNotificationBatch(ids);
        assertNull(notificationService.getById(n1.getId()));
        assertNull(notificationService.getById(n2.getId()));
    }

    @Test
    public void testUserNotifications() {
        for (int i = 0; i < 15; i++) {
            Notification notification = new Notification();
            notification.setTitle("用户通知" + i);
            notification.setContent("内容" + i);
            notification.setType(1);
            notification.setChannel(1);
            notification.setUserId(4L);
            notification.setReadStatus(0);
            notification.setSendStatus(2);
            notificationService.save(notification);
        }
        
        IPage<com.aiready.notification.dto.NotificationDTO> page = 
                notificationService.getUserNotifications(4L, 1, 10);
        assertNotNull(page);
        assertEquals(10, page.getRecords().size());
        assertTrue(page.getTotal() >= 15);
    }
}
