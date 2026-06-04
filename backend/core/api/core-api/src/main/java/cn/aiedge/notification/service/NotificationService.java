package cn.aiedge.notification.service;

import cn.aiedge.notification.entity.NotificationRecord;
import cn.aiedge.notification.entity.NotificationTemplate;
import cn.aiedge.notification.model.Notification;

import java.util.List;
import java.util.Map;

/**
 * 通知服务接口
 * 
 * @author AI-Ready Team
 * @since 1.1.0
 */
public interface NotificationService {
    
    // ==================== 模板管理 ====================
    
    /**
     * 创建模板
     */
    NotificationTemplate createTemplate(NotificationTemplate template);
    
    /**
     * 更新模板
     */
    NotificationTemplate updateTemplate(NotificationTemplate template);
    
    /**
     * 删除模板
     */
    boolean deleteTemplate(Long templateId);
    
    /**
     * 根据ID获取模板
     */
    NotificationTemplate getTemplate(Long templateId);
    
    /**
     * 根据编码获取模板
     */
    NotificationTemplate getTemplateByCode(String templateCode);
    
    /**
     * 获取所有启用的模板
     */
    List<NotificationTemplate> getAllTemplates();
    
    /**
     * 根据类型获取模板
     */
    List<NotificationTemplate> getTemplatesByType(String notifyType);
    
    // ==================== 通知发送 ====================
    
    /**
     * 发送通知（增强版）
     */
    NotificationRecord send(String templateCode, Long receiverId, String receiverType,
                            String receiverAddress, Map<String, Object> variables);
    
    /**
     * 发送站内消息
     */
    NotificationRecord sendSiteMessage(Long receiverId, String title, String content);
    
    /**
     * 发送邮件
     */
    NotificationRecord sendEmail(String email, String title, String content);
    
    /**
     * 发送短信
     */
    NotificationRecord sendSms(String phone, String content);
    
    /**
     * 批量发送
     */
    List<NotificationRecord> sendBatch(String templateCode, List<Long> receiverIds,
                                       String receiverType, Map<String, Object> variables);
    
    // ==================== 通知记录管理 ====================
    
    /**
     * 获取用户通知列表
     */
    List<NotificationRecord> getUserNotifications(Long userId, Integer readStatus, int limit);
    
    /**
     * 获取未读数量
     */
    int getUnreadCount(Long userId);
    
    /**
     * 标记已读
     */
    boolean markAsRead(Long recordId);
    
    /**
     * 全部标记已读
     */
    int markAllAsRead(Long userId);
    
    /**
     * 删除通知
     */
    boolean deleteNotification(Long recordId);

    /**
     * 批量删除通知
     */
    boolean deleteNotifications(List<Long> ids);

    /**
     * 获取所有通知记录（用于导出）
     */
    List<NotificationRecord> listAllRecords();

    // ==================== 重试与队列处理 ====================
    
    /**
     * 重试发送
     */
    boolean retry(Long recordId);
    
    /**
     * 处理待发送通知
     */
    int processPendingNotifications();
}
