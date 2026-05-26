package com.aiready.notification.service;

import com.aiready.notification.dto.NotificationDTO;
import com.aiready.notification.dto.NotificationQueryRequest;
import com.aiready.notification.dto.SendNotificationRequest;
import com.aiready.notification.entity.Notification;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 消息通知服务接口
 */
public interface NotificationService extends IService<Notification> {

    /**
     * 发送消息通知
     */
    void sendNotification(SendNotificationRequest request);

    /**
     * 发送站内信
     */
    void sendInSiteMessage(Long userId, String title, String content);

    /**
     * 发送邮件
     */
    void sendEmail(String toEmail, String subject, String content);

    /**
     * 发送短信
     */
    void sendSms(String phone, String templateCode, String templateParams);

    /**
     * 使用模板发送通知
     */
    void sendWithTemplate(Long userId, String templateCode, Object params);

    /**
     * 获取用户消息列表
     */
    IPage<NotificationDTO> getUserNotifications(Long userId, Integer page, Integer size);

    /**
     * 获取用户未读消息数量
     */
    Long getUnreadCount(Long userId);

    /**
     * 标记消息为已读
     */
    void markAsRead(Long notificationId);

    /**
     * 批量标记消息为已读
     */
    void markAsReadBatch(List<Long> notificationIds);

    /**
     * 标记用户所有消息为已读
     */
    void markAllAsRead(Long userId);

    /**
     * 查询消息列表
     */
    IPage<NotificationDTO> queryNotifications(NotificationQueryRequest request);

    /**
     * 删除消息
     */
    void deleteNotification(Long notificationId);

    /**
     * 批量删除消息
     */
    void deleteNotificationBatch(List<Long> notificationIds);
}
