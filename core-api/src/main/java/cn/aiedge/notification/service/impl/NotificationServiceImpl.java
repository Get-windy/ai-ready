package cn.aiedge.notification.service.impl;

import cn.aiedge.cache.service.CacheService;
import cn.aiedge.notification.model.Notification;
import cn.aiedge.notification.model.NotificationTemplate;
import cn.aiedge.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 通知服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final CacheService cacheService;
    
    private static final String NOTIFICATION_KEY = "notification:";
    private static final String USER_NOTIFICATION_KEY = "notification:user:";
    private static final String TEMPLATE_KEY = "notification:template:";
    
    // 内置通知模板
    private static final Map<String, NotificationTemplate> BUILTIN_TEMPLATES = new LinkedHashMap<>();
    
    static {
        // 审批通知
        NotificationTemplate approvalTpl = new NotificationTemplate();
        approvalTpl.setTemplateCode("approval_pending");
        approvalTpl.setTemplateName("待审批通知");
        approvalTpl.setNotificationType("approval");
        approvalTpl.setChannel("in_app");
        approvalTpl.setTitleTemplate("您有一条待审批的${businessType}");
        approvalTpl.setContentTemplate("${applicantName}提交的${businessType}需要您审批，请及时处理。");
        approvalTpl.setVariables("businessType,applicantName");
        approvalTpl.setEnabled(true);
        BUILTIN_TEMPLATES.put("approval_pending", approvalTpl);
        
        // 审批结果通知
        NotificationTemplate approvalResult = new NotificationTemplate();
        approvalResult.setTemplateCode("approval_result");
        approvalResult.setTemplateName("审批结果通知");
        approvalResult.setNotificationType("approval");
        approvalResult.setChannel("in_app");
        approvalResult.setTitleTemplate("您的申请已${result}");
        approvalResult.setContentTemplate("您提交的${businessType}已被${approverName}${result}。");
        approvalResult.setVariables("result,businessType,approverName");
        approvalResult.setEnabled(true);
        BUILTIN_TEMPLATES.put("approval_result", approvalResult);
        
        // 系统公告
        NotificationTemplate systemTpl = new NotificationTemplate();
        systemTpl.setTemplateCode("system_notice");
        systemTpl.setTemplateName("系统公告");
        systemTpl.setNotificationType("system");
        systemTpl.setChannel("in_app");
        systemTpl.setTitleTemplate("${title}");
        systemTpl.setContentTemplate("${content}");
        systemTpl.setVariables("title,content");
        systemTpl.setEnabled(true);
        BUILTIN_TEMPLATES.put("system_notice", systemTpl);
    }

    @Override
    public Notification sendNotification(Notification notification, Long tenantId) {
        notification.setNotificationId(System.currentTimeMillis());
        notification.setSendTime(LocalDateTime.now());
        notification.setStatus("sent");
        notification.setTenantId(tenantId);
        
        // 保存通知
        String key = NOTIFICATION_KEY + notification.getNotificationId();
        cacheService.set(key, notification);
        
        // 添加到用户通知列表
        String userKey = USER_NOTIFICATION_KEY + notification.getReceiverId();
        cacheService.lPush(userKey, notification.getNotificationId().toString());
        
        log.info("发送通知: id={}, receiver={}, title={}", 
                notification.getNotificationId(), notification.getReceiverId(), notification.getTitle());
        
        // 根据渠道推送
        pushToUser(notification.getReceiverId(), notification.getChannel(), notification);
        
        return notification;
    }

    @Override
    public Notification sendNotification(String templateCode, Long receiverId, 
                                         Map<String, Object> variables, Long tenantId) {
        NotificationTemplate template = getTemplate(templateCode);
        if (template == null) {
            throw new RuntimeException("通知模板不存在: " + templateCode);
        }
        
        // 渲染模板
        String title = renderTemplate(template.getTitleTemplate(), variables);
        String content = renderTemplate(template.getContentTemplate(), variables);
        
        Notification notification = new Notification();
        notification.setNotificationType(template.getNotificationType());
        notification.setChannel(template.getChannel());
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReceiverId(receiverId);
        notification.setTenantId(tenantId);
        
        return sendNotification(notification, tenantId);
    }

    @Override
    public void sendBatchNotifications(List<Notification> notifications, Long tenantId) {
        for (Notification notification : notifications) {
            sendNotification(notification, tenantId);
        }
        log.info("批量发送通知: count={}", notifications.size());
    }

    @Override
    public Notification getNotification(Long notificationId) {
        String key = NOTIFICATION_KEY + notificationId;
        return cacheService.get(key, Notification.class);
    }

    @Override
    public List<Notification> getUnreadNotifications(Long userId, Long tenantId) {
        List<Notification> result = new ArrayList<>();
        String userKey = USER_NOTIFICATION_KEY + userId;
        List<Object> ids = cacheService.lRange(userKey, 0, 100);
        
        if (ids != null) {
            for (Object id : ids) {
                Notification notification = getNotification(Long.parseLong(id.toString()));
                if (notification != null && "sent".equals(notification.getStatus())) {
                    result.add(notification);
                }
            }
        }
        return result;
    }

    @Override
    public List<Notification> getUserNotifications(Long userId, int page, int pageSize, Long tenantId) {
        List<Notification> result = new ArrayList<>();
        String userKey = USER_NOTIFICATION_KEY + userId;
        int start = (page - 1) * pageSize;
        int end = start + pageSize - 1;
        List<Object> ids = cacheService.lRange(userKey, start, end);
        
        if (ids != null) {
            for (Object id : ids) {
                Notification notification = getNotification(Long.parseLong(id.toString()));
                if (notification != null) {
                    result.add(notification);
                }
            }
        }
        return result;
    }

    @Override
    public int getUnreadCount(Long userId, Long tenantId) {
        return getUnreadNotifications(userId, tenantId).size();
    }

    @Override
    public boolean markAsRead(Long notificationId, Long userId) {
        Notification notification = getNotification(notificationId);
        if (notification == null || !notification.getReceiverId().equals(userId)) {
            return false;
        }
        
        notification.setStatus("read");
        notification.setReadTime(LocalDateTime.now());
        
        String key = NOTIFICATION_KEY + notificationId;
        cacheService.set(key, notification);
        return true;
    }

    @Override
    public boolean markAllAsRead(Long userId, Long tenantId) {
        List<Notification> unread = getUnreadNotifications(userId, tenantId);
        for (Notification notification : unread) {
            markAsRead(notification.getNotificationId(), userId);
        }
        log.info("标记全部已读: userId={}, count={}", userId, unread.size());
        return true;
    }

    @Override
    public boolean deleteNotification(Long notificationId, Long userId) {
        Notification notification = getNotification(notificationId);
        if (notification == null || !notification.getReceiverId().equals(userId)) {
            return false;
        }
        
        String key = NOTIFICATION_KEY + notificationId;
        cacheService.delete(key);
        return true;
    }

    @Override
    public boolean deleteAllRead(Long userId, Long tenantId) {
        String userKey = USER_NOTIFICATION_KEY + userId;
        List<Object> ids = cacheService.lRange(userKey, 0, -1);
        
        if (ids != null) {
            for (Object id : ids) {
                Notification notification = getNotification(Long.parseLong(id.toString()));
                if (notification != null && "read".equals(notification.getStatus())) {
                    deleteNotification(notification.getNotificationId(), userId);
                }
            }
        }
        return true;
    }

    @Override
    public NotificationTemplate getTemplate(String templateCode) {
        NotificationTemplate builtin = BUILTIN_TEMPLATES.get(templateCode);
        if (builtin != null) return builtin;
        
        return cacheService.get(TEMPLATE_KEY + templateCode, NotificationTemplate.class);
    }

    @Override
    public List<NotificationTemplate> getTemplates(String channel, Long tenantId) {
        List<NotificationTemplate> result = new ArrayList<>(BUILTIN_TEMPLATES.values());
        return result;
    }

    @Override
    public NotificationTemplate saveTemplate(NotificationTemplate template) {
        template.setCreateTime(LocalDateTime.now());
        cacheService.set(TEMPLATE_KEY + template.getTemplateCode(), template);
        return template;
    }

    @Override
    public void sendEmail(String to, String subject, String content) {
        log.info("发送邮件: to={}, subject={}", to, subject);
        // 实际实现需要邮件服务
    }

    @Override
    public void sendSms(String phone, String content) {
        log.info("发送短信: phone={}", phone);
        // 实际实现需要短信服务
    }

    @Override
    public void sendWechat(String openId, String content) {
        log.info("发送微信消息: openId={}", openId);
        // 实际实现需要微信服务
    }

    @Override
    public void pushToUser(Long userId, String channel, Notification notification) {
        log.info("推送通知: userId={}, channel={}", userId, channel);
        // 根据渠道推送
        if (channel == null || "in_app".equals(channel)) {
            // 站内信已通过缓存保存
            return;
        }
        
        switch (channel) {
            case "email":
                // 需要用户邮箱信息
                break;
            case "sms":
                // 需要用户手机号
                break;
            case "wechat":
                // 需要用户openId
                break;
            case "websocket":
                // 实时推送
                break;
        }
    }
    
    private String renderTemplate(String template, Map<String, Object> variables) {
        if (template == null || variables == null) return template;
        
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", 
                    entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return result;
    }
}
