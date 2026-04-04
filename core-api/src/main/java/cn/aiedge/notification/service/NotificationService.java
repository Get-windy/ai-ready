package cn.aiedge.notification.service;

import cn.aiedge.notification.model.Notification;
import cn.aiedge.notification.model.NotificationTemplate;
import java.util.List;
import java.util.Map;

/**
 * 通知服务接口
 */
public interface NotificationService {
    
    // 发送通知
    Notification sendNotification(Notification notification, Long tenantId);
    Notification sendNotification(String templateCode, Long receiverId, 
                                  Map<String, Object> variables, Long tenantId);
    void sendBatchNotifications(List<Notification> notifications, Long tenantId);
    
    // 获取通知
    Notification getNotification(Long notificationId);
    List<Notification> getUnreadNotifications(Long userId, Long tenantId);
    List<Notification> getUserNotifications(Long userId, int page, int pageSize, Long tenantId);
    int getUnreadCount(Long userId, Long tenantId);
    
    // 标记已读
    boolean markAsRead(Long notificationId, Long userId);
    boolean markAllAsRead(Long userId, Long tenantId);
    
    // 删除通知
    boolean deleteNotification(Long notificationId, Long userId);
    boolean deleteAllRead(Long userId, Long tenantId);
    
    // 模板管理
    NotificationTemplate getTemplate(String templateCode);
    List<NotificationTemplate> getTemplates(String channel, Long tenantId);
    NotificationTemplate saveTemplate(NotificationTemplate template);
    
    // 多渠道推送
    void sendEmail(String to, String subject, String content);
    void sendSms(String phone, String content);
    void sendWechat(String openId, String content);
    void pushToUser(Long userId, String channel, Notification notification);
}
