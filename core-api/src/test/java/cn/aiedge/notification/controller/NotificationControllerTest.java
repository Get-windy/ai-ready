package cn.aiedge.notification.controller;

import cn.aiedge.notification.model.Notification;
import cn.aiedge.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 通知控制器单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setTitle("测试通知");
        testNotification.setContent("测试内容");
        testNotification.setReceiverId(100L);
    }

    @Test
    @DisplayName("获取未读通知列表")
    void testGetUnreadNotifications() throws Exception {
        List<Notification> notifications = new ArrayList<>();
        notifications.add(testNotification);
        
        when(notificationService.getUnreadNotifications(anyLong(), anyLong()))
            .thenReturn(notifications);

        mockMvc.perform(get("/api/notification/unread")
                .header("X-User-Id", "100")
                .header("X-Tenant-Id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("获取通知列表")
    void testGetNotifications() throws Exception {
        List<Notification> notifications = new ArrayList<>();
        
        when(notificationService.getUserNotifications(anyLong(), any(), any(), anyLong()))
            .thenReturn(notifications);

        mockMvc.perform(get("/api/notification/list")
                .param("page", "1")
                .param("pageSize", "10")
                .header("X-User-Id", "100")
                .header("X-Tenant-Id", "1"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取未读数量")
    void testGetUnreadCount() throws Exception {
        when(notificationService.getUnreadCount(anyLong(), anyLong())).thenReturn(5);

        mockMvc.perform(get("/api/notification/unread-count")
                .header("X-User-Id", "100")
                .header("X-Tenant-Id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    @DisplayName("标记已读")
    void testMarkAsRead() throws Exception {
        when(notificationService.markAsRead(anyLong(), anyLong())).thenReturn(true);

        mockMvc.perform(put("/api/notification/1/read")
                .header("X-User-Id", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("标记全部已读")
    void testMarkAllAsRead() throws Exception {
        when(notificationService.markAllAsRead(anyLong(), anyLong())).thenReturn(true);

        mockMvc.perform(put("/api/notification/read-all")
                .header("X-User-Id", "100")
                .header("X-Tenant-Id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("删除通知")
    void testDeleteNotification() throws Exception {
        when(notificationService.deleteNotification(anyLong(), anyLong())).thenReturn(true);

        mockMvc.perform(delete("/api/notification/1")
                .header("X-User-Id", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("发送通知")
    void testSendNotification() throws Exception {
        when(notificationService.sendNotification(any(), anyLong())).thenReturn(testNotification);

        mockMvc.perform(post("/api/notification/send")
                .header("X-Tenant-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testNotification)))
            .andExpect(status().isOk());
    }
}
