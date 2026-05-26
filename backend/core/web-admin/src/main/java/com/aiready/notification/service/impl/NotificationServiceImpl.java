package com.aiready.notification.service.impl;

import com.aiready.notification.dto.NotificationDTO;
import com.aiready.notification.dto.NotificationQueryRequest;
import com.aiready.notification.dto.SendNotificationRequest;
import com.aiready.notification.entity.Notification;
import com.aiready.notification.mapper.NotificationMapper;
import com.aiready.notification.service.EmailService;
import com.aiready.notification.service.NotificationService;
import com.aiready.notification.service.NotificationTemplateService;
import com.aiready.notification.service.SmsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> 
        implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final NotificationTemplateService templateService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendNotification(SendNotificationRequest request) {
        if (CollectionUtils.isEmpty(request.getUserIds())) {
            log.warn("发送通知失败：接收用户ID列表为空");
            return;
        }
        Integer channel = request.getChannel();
        for (Long userId : request.getUserIds()) {
            try {
                if (channel == 1 || channel == 4) saveInSiteNotification(userId, request);
                if (channel == 2 || channel == 4) { }
                if (channel == 3 || channel == 4) { }
            } catch (Exception e) {
                log.error("发送通知给用户{}失败: {}", userId, e.getMessage());
            }
        }
    }

    private void saveInSiteNotification(Long userId, SendNotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(request.getTitle());
        notification.setContent(request.getContent());
        notification.setType(request.getType());
        notification.setChannel(1);
        notification.setSenderId(0L);
        notification.setSenderName("系统");
        notification.setBizType(request.getBizType());
        notification.setBizId(request.getBizId());
        notification.setReadStatus(0);
        notification.setSendStatus(2);
        notification.setSendTime(LocalDateTime.now());
        if (request.getTemplateParams() != null) {
            try {
                notification.setTemplateParams(objectMapper.writeValueAsString(request.getTemplateParams()));
            } catch (Exception e) {
                log.error("序列化模板参数失败", e);
            }
        }
        save(notification);
    }

    @Override
    public void sendInSiteMessage(Long userId, String title, String content) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(1);
        notification.setChannel(1);
        notification.setSenderId(0L);
        notification.setSenderName("系统");
        notification.setReadStatus(0);
        notification.setSendStatus(2);
        notification.setSendTime(LocalDateTime.now());
        save(notification);
    }

    @Override
    public void sendEmail(String toEmail, String subject, String content) {
        emailService.sendSimpleEmail(toEmail, subject, content);
    }

    @Override
    public void sendSms(String phone, String templateCode, String templateParams) { }

    @Override
    public void sendWithTemplate(Long userId, String templateCode, Object params) { }

    @Override
    public IPage<NotificationDTO> getUserNotifications(Long userId, Integer page, Integer size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId).eq(Notification::getDeleted, 0).orderByDesc(Notification::getCreateTime);
        IPage<Notification> notificationPage = page(new Page<>(page, size), wrapper);
        return notificationPage.convert(this::convertToDTO);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        return notificationMapper.selectUnreadCount(userId);
    }

    @Override
    public void markAsRead(Long notificationId) {
        notificationMapper.markAsRead(notificationId);
    }

    @Override
    public void markAsReadBatch(List<Long> notificationIds) {
        if (!CollectionUtils.isEmpty(notificationIds)) {
            notificationMapper.markAsReadBatch(notificationIds);
        }
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationMapper.markAllAsRead(userId);
    }

    @Override
    public IPage<NotificationDTO> queryNotifications(NotificationQueryRequest request) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        if (request.getUserId() != null) wrapper.eq(Notification::getUserId, request.getUserId());
        if (request.getType() != null) wrapper.eq(Notification::getType, request.getType());
        if (request.getChannel() != null) wrapper.eq(Notification::getChannel, request.getChannel());
        if (request.getReadStatus() != null) wrapper.eq(Notification::getReadStatus, request.getReadStatus());
        if (request.getSendStatus() != null) wrapper.eq(Notification::getSendStatus, request.getSendStatus());
        if (StringUtils.hasText(request.getBizType())) wrapper.eq(Notification::getBizType, request.getBizType());
        if (request.getStartTime() != null) wrapper.ge(Notification::getCreateTime, request.getStartTime());
        if (request.getEndTime() != null) wrapper.le(Notification::getCreateTime, request.getEndTime());
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(Notification::getTitle, request.getKeyword()).or().like(Notification::getContent, request.getKeyword()));
        }
        wrapper.eq(Notification::getDeleted, 0).orderByDesc(Notification::getCreateTime);
        IPage<Notification> notificationPage = page(new Page<>(request.getPage(), request.getSize()), wrapper);
        return notificationPage.convert(this::convertToDTO);
    }

    @Override
    public void deleteNotification(Long notificationId) {
        removeById(notificationId);
    }

    @Override
    public void deleteNotificationBatch(List<Long> notificationIds) {
        if (!CollectionUtils.isEmpty(notificationIds)) {
            removeByIds(notificationIds);
        }
    }

    private NotificationDTO convertToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        BeanUtils.copyProperties(notification, dto);
        return dto;
    }
}
