package cn.aiedge.notification.controller;

import cn.aiedge.notification.model.Notification;
import cn.aiedge.notification.model.NotificationTemplate;
import cn.aiedge.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@Tag(name = "通知服务", description = "通知消息管理功能")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/unread")
    @Operation(summary = "获取未读通知")
    public ResponseEntity<Map<String, Object>> getUnreadNotifications(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        List<Notification> notifications = notificationService.getUnreadNotifications(userId, tenantId);
        int count = notificationService.getUnreadCount(userId, tenantId);
        
        return ResponseEntity.ok(Map.of("notifications", notifications, "unreadCount", count));
    }

    @GetMapping("/list")
    @Operation(summary = "获取通知列表")
    public ResponseEntity<Map<String, Object>> getUserNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        List<Notification> notifications = notificationService.getUserNotifications(userId, page, pageSize, tenantId);
        int unreadCount = notificationService.getUnreadCount(userId, tenantId);
        
        return ResponseEntity.ok(Map.of("notifications", notifications, "unreadCount", unreadCount, "page", page, "pageSize", pageSize));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读数量")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        int count = notificationService.getUnreadCount(userId, tenantId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/{notificationId}/read")
    @Operation(summary = "标记已读")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable Long notificationId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        boolean success = notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @PostMapping("/read-all")
    @Operation(summary = "全部标记已读")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        notificationService.markAllAsRead(userId, tenantId);
        return ResponseEntity.ok(Map.of("success", true, "message", "已全部标记已读"));
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "删除通知")
    public ResponseEntity<Map<String, Object>> deleteNotification(
            @PathVariable Long notificationId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        boolean success = notificationService.deleteNotification(notificationId, userId);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @DeleteMapping("/read")
    @Operation(summary = "删除已读通知")
    public ResponseEntity<Map<String, Object>> deleteAllRead(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        notificationService.deleteAllRead(userId, tenantId);
        return ResponseEntity.ok(Map.of("success", true, "message", "已删除所有已读通知"));
    }

    @PostMapping("/send")
    @Operation(summary = "发送通知")
    public ResponseEntity<Map<String, Object>> sendNotification(
            @RequestBody Notification notification,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        Notification sent = notificationService.sendNotification(notification, tenantId);
        return ResponseEntity.ok(Map.of("success", true, "notification", sent));
    }

    @PostMapping("/send-template")
    @Operation(summary = "使用模板发送通知")
    public ResponseEntity<Map<String, Object>> sendTemplateNotification(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        String templateCode = (String) request.get("templateCode");
        Long receiverId = Long.parseLong(request.get("receiverId").toString());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) request.get("variables");
        
        Notification sent = notificationService.sendNotification(templateCode, receiverId, variables, tenantId);
        return ResponseEntity.ok(Map.of("success", true, "notification", sent));
    }

    @GetMapping("/templates")
    @Operation(summary = "获取通知模板")
    public ResponseEntity<List<NotificationTemplate>> getTemplates(
            @RequestParam(required = false) String channel,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        return ResponseEntity.ok(notificationService.getTemplates(channel, tenantId));
    }

    @PostMapping("/templates")
    @Operation(summary = "保存通知模板")
    public ResponseEntity<NotificationTemplate> saveTemplate(@RequestBody NotificationTemplate template) {
        NotificationTemplate saved = notificationService.saveTemplate(template);
        return ResponseEntity.ok(saved);
    }
}
