package com.aiready.notification.controller;

import com.aiready.notification.dto.NotificationDTO;
import com.aiready.notification.dto.NotificationQueryRequest;
import com.aiready.notification.dto.SendNotificationRequest;
import com.aiready.notification.service.NotificationService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    public void sendNotification(@RequestBody SendNotificationRequest request) {
        notificationService.sendNotification(request);
    }

    @PostMapping("/send-in-site")
    public void sendInSiteMessage(@RequestParam Long userId, 
                                   @RequestParam String title, 
                                   @RequestParam String content) {
        notificationService.sendInSiteMessage(userId, title, content);
    }

    @GetMapping("/user/{userId}")
    public IPage<NotificationDTO> getUserNotifications(@PathVariable Long userId,
                                                        @RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "10") Integer size) {
        return notificationService.getUserNotifications(userId, page, size);
    }

    @GetMapping("/unread-count/{userId}")
    public Long getUnreadCount(@PathVariable Long userId) {
        return notificationService.getUnreadCount(userId);
    }

    @PostMapping("/read/{notificationId}")
    public void markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
    }

    @PostMapping("/read-batch")
    public void markAsReadBatch(@RequestBody List<Long> notificationIds) {
        notificationService.markAsReadBatch(notificationIds);
    }

    @PostMapping("/read-all/{userId}")
    public void markAllAsRead(@PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
    }

    @PostMapping("/query")
    public IPage<NotificationDTO> queryNotifications(@RequestBody NotificationQueryRequest request) {
        return notificationService.queryNotifications(request);
    }

    @DeleteMapping("/{notificationId}")
    public void deleteNotification(@PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
    }

    @DeleteMapping("/batch")
    public void deleteNotificationBatch(@RequestBody List<Long> notificationIds) {
        notificationService.deleteNotificationBatch(notificationIds);
    }

    @GetMapping("/export")
    public List<com.aiready.notification.entity.Notification> export() {
        return notificationService.list();
    }
}
