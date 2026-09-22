package cn.aiedge.notification.controller;

import cn.aiedge.notification.entity.NotificationRecord;
import cn.aiedge.notification.entity.NotificationTemplate;
import cn.aiedge.notification.model.Notification;
import cn.aiedge.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 通知控制器
 *
 * <p><b>⚠️ 2026-09-22 修：用户身份一律取会话，不再读请求头 `X-User-Id`。</b>
 * 原实现每个方法都接 {@code @RequestHeader("X-User-Id") Long userId} 并直接拿它查数据，
 * 有两处后果：</p>
 * <ol>
 *   <li><b>越权面</b>：该头由客户端随意填写 ⇒ 任何登录用户改个头就能读/删
 *       <b>别人</b>的通知（服务层只按 userId 过滤，没有与会话核对）。</li>
 *   <li><b>功能实际是空的</b>：前端 <b>从未发送</b>这个头（实测 pc-admin 全仓 0 处引用）
 *       ⇒ {@code userId} 恒为 null，查询结果恒空。也就是说这个头既没带来安全，
 *       也没带来功能，纯属历史遗留。</li>
 * </ol>
 * <p>现在改为从 Sa-Token 会话取当前用户 id（{@link #sessionUserId()}），
 * 两个问题一并消除；对外接口形状不变。</p>
 */
@RestController("coreNotificationController")
@RequestMapping("/api/core/notification")
@RequiredArgsConstructor
@Tag(name = "通知服务", description = "通知消息管理功能")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 当前登录用户 id —— 通知类接口的**唯一**身份来源。
     *
     * <p>不要退回成读请求头：那样既能被伪造（读写他人通知），
     * 又会因为前端根本不发这个头而让功能恒空（历史实情，见类注释）。</p>
     */
    private Long sessionUserId() {
        return cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
    }

    @GetMapping("/unread")
    @Operation(summary = "获取未读通知")
    public ResponseEntity<Map<String, Object>> getUnreadNotifications(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        List<NotificationRecord> notifications = notificationService.getUserNotifications(sessionUserId(), 0, 20);
        int count = notificationService.getUnreadCount(sessionUserId());
        
        return ResponseEntity.ok(Map.of("notifications", notifications, "unreadCount", count));
    }

    @GetMapping("/list")
    @Operation(summary = "获取通知列表")
    public ResponseEntity<Map<String, Object>> getUserNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        List<NotificationRecord> notifications = notificationService.getUserNotifications(sessionUserId(), null, pageSize);
        int unreadCount = notificationService.getUnreadCount(sessionUserId());
        
        return ResponseEntity.ok(Map.of("notifications", notifications, "unreadCount", unreadCount, "page", page, "pageSize", pageSize));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读数量")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        int count = notificationService.getUnreadCount(sessionUserId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/{notificationId}/read")
    @Operation(summary = "标记已读")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable Long notificationId) {
        
        boolean success = notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @PostMapping("/read-all")
    @Operation(summary = "全部标记已读")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        notificationService.markAllAsRead(sessionUserId());
        return ResponseEntity.ok(Map.of("success", true, "message", "已全部标记已读"));
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "删除通知")
    public ResponseEntity<Map<String, Object>> deleteNotification(
            @PathVariable Long notificationId) {

        boolean success = notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除通知")
    public ResponseEntity<Map<String, Object>> batchDelete(@RequestBody List<Long> ids) {
        boolean success = notificationService.deleteNotifications(ids);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @SaCheckPermission("notification:message:export")
    @GetMapping("/export")
    @Operation(summary = "导出通知记录")
    public ResponseEntity<List<NotificationRecord>> export() {
        return ResponseEntity.ok(notificationService.listAllRecords());
    }

    @DeleteMapping("/read")
    @Operation(summary = "删除已读通知")
    public ResponseEntity<Map<String, Object>> deleteAllRead(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        return ResponseEntity.ok(Map.of("success", true, "message", "已删除所有已读通知"));
    }

    @SaCheckPermission("notification:message:send")
    @PostMapping("/send")
    @Operation(summary = "发送通知")
    public ResponseEntity<Map<String, Object>> sendNotification(
            @RequestBody Notification notification,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        NotificationRecord sent = notificationService.sendSiteMessage(null, notification.getTitle(), notification.getContent());
        return ResponseEntity.ok(Map.of("success", true, "notification", sent));
    }

    @SaCheckPermission("notification:message:send")
    @PostMapping("/send-template")
    @Operation(summary = "使用模板发送通知")
    public ResponseEntity<Map<String, Object>> sendTemplateNotification(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        String templateCode = (String) request.get("templateCode");
        Long receiverId = Long.parseLong(request.get("receiverId").toString());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) request.get("variables");
        
        NotificationRecord sent = notificationService.send(templateCode, receiverId, null, null, variables);
        return ResponseEntity.ok(Map.of("success", true, "notification", sent));
    }

    @SaCheckPermission("notification:template:list")
    @GetMapping("/templates")
    @Operation(summary = "获取通知模板")
    public ResponseEntity<List<NotificationTemplate>> getTemplates(
            @RequestParam(required = false) String channel,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        return ResponseEntity.ok(notificationService.getTemplatesByType(channel));
    }

    @SaCheckPermission("notification:template:create")
    @PostMapping("/templates")
    @Operation(summary = "保存通知模板")
    public ResponseEntity<NotificationTemplate> saveTemplate(@RequestBody NotificationTemplate template) {
        NotificationTemplate saved = notificationService.createTemplate(template);
        return ResponseEntity.ok(saved);
    }
}
