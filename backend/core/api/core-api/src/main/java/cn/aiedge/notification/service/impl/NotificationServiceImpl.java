package cn.aiedge.notification.service.impl;

import cn.aiedge.notification.entity.NotificationRecord;
import cn.aiedge.notification.entity.NotificationTemplate;
import cn.aiedge.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知服务实现 - 实现新的NotificationService接口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    // 内存存储（实际应使用数据库）
    private final Map<Long, NotificationTemplate> templates = new ConcurrentHashMap<>();
    private final Map<Long, NotificationRecord> records = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> userNotifications = new ConcurrentHashMap<>();

    // ==================== 模板管理 ====================

    @Override
    public NotificationTemplate createTemplate(NotificationTemplate template) {
        if (template.getId() == null) {
            template.setId(System.currentTimeMillis());
        }
        template.setCreateTime(LocalDateTime.now());
        templates.put(template.getId(), template);
        log.info("创建通知模板: id={}, code={}", template.getId(), template.getTemplateCode());
        return template;
    }

    @Override
    public NotificationTemplate updateTemplate(NotificationTemplate template) {
        if (template.getId() == null || !templates.containsKey(template.getId())) {
            throw new RuntimeException("模板不存在");
        }
        template.setUpdateTime(LocalDateTime.now());
        templates.put(template.getId(), template);
        log.info("更新通知模板: id={}", template.getId());
        return template;
    }

    @Override
    public boolean deleteTemplate(Long templateId) {
        if (templates.remove(templateId) != null) {
            log.info("删除通知模板: id={}", templateId);
            return true;
        }
        return false;
    }

    @Override
    public NotificationTemplate getTemplate(Long templateId) {
        return templates.get(templateId);
    }

    @Override
    public NotificationTemplate getTemplateByCode(String templateCode) {
        return templates.values().stream()
                .filter(t -> templateCode.equals(t.getTemplateCode()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<NotificationTemplate> getAllTemplates() {
        return new ArrayList<>(templates.values());
    }

    @Override
    public List<NotificationTemplate> getTemplatesByType(String notifyType) {
        return templates.values().stream()
                .filter(t -> notifyType.equals(t.getNotifyType()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    // ==================== 通知发送 ====================

    @Override
    public NotificationRecord send(String templateCode, Long receiverId, String receiverType,
                                    String receiverAddress, Map<String, Object> variables) {
        NotificationTemplate template = getTemplateByCode(templateCode);
        if (template == null) {
            throw new RuntimeException("模板不存在: " + templateCode);
        }

        NotificationRecord record = new NotificationRecord();
        record.setId(System.currentTimeMillis());
        record.setTemplateId(template.getId());
        record.setTemplateCode(templateCode);
        record.setReceiverId(receiverId);
        record.setReceiverType(receiverType);
        record.setReceiverAddress(receiverAddress);
        record.setTitle(renderTemplate(template.getTitle(), variables));
        record.setContent(renderTemplate(template.getContent(), variables));
        record.setNotifyType(template.getNotifyType());
        record.setStatus(0);
        record.setCreateTime(LocalDateTime.now());

        saveAndSend(record);
        return record;
    }

    @Override
    public NotificationRecord sendSiteMessage(Long receiverId, String title, String content) {
        NotificationRecord record = new NotificationRecord();
        record.setId(System.currentTimeMillis());
        record.setReceiverId(receiverId);
        record.setReceiverType("user");
        record.setTitle(title);
        record.setContent(content);
        record.setNotifyType("site");
        record.setStatus(1);
        record.setCreateTime(LocalDateTime.now());

        saveAndSend(record);
        log.info("发送站内消息: receiverId={}, title={}", receiverId, title);
        return record;
    }

    @Override
    public NotificationRecord sendEmail(String email, String title, String content) {
        NotificationRecord record = new NotificationRecord();
        record.setId(System.currentTimeMillis());
        record.setReceiverAddress(email);
        record.setTitle(title);
        record.setContent(content);
        record.setNotifyType("email");
        record.setStatus(1);
        record.setCreateTime(LocalDateTime.now());

        saveAndSend(record);
        log.info("发送邮件: to={}, title={}", email, title);
        return record;
    }

    @Override
    public NotificationRecord sendSms(String phone, String content) {
        NotificationRecord record = new NotificationRecord();
        record.setId(System.currentTimeMillis());
        record.setReceiverAddress(phone);
        record.setContent(content);
        record.setNotifyType("sms");
        record.setStatus(1);
        record.setCreateTime(LocalDateTime.now());

        saveAndSend(record);
        log.info("发送短信: phone={}", phone);
        return record;
    }

    @Override
    public List<NotificationRecord> sendBatch(String templateCode, List<Long> receiverIds,
                                               String receiverType, Map<String, Object> variables) {
        List<NotificationRecord> results = new ArrayList<>();
        for (Long receiverId : receiverIds) {
            try {
                results.add(send(templateCode, receiverId, receiverType, null, variables));
            } catch (Exception e) {
                log.error("批量发送失败: receiverId={}", receiverId, e);
            }
        }
        return results;
    }

    // ==================== 通知记录管理 ====================

    @Override
    public List<NotificationRecord> getUserNotifications(Long userId, Integer readStatus, int limit) {
        List<Long> ids = userNotifications.getOrDefault(userId, new ArrayList<>());
        List<NotificationRecord> result = new ArrayList<>();
        
        for (int i = ids.size() - 1; i >= 0 && result.size() < limit; i--) {
            NotificationRecord record = records.get(ids.get(i));
            if (record != null) {
                if (readStatus == null || readStatus.equals(getReadStatusValue(record))) {
                    result.add(record);
                }
            }
        }
        return result;
    }

    @Override
    public int getUnreadCount(Long userId) {
        List<Long> ids = userNotifications.getOrDefault(userId, new ArrayList<>());
        int count = 0;
        for (Long id : ids) {
            NotificationRecord record = records.get(id);
            if (record != null && Integer.valueOf(0).equals(record.getReadStatus())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean markAsRead(Long recordId) {
        NotificationRecord record = records.get(recordId);
        if (record == null) return false;
        record.setReadStatus(1);
        record.setReadTime(LocalDateTime.now());
        return true;
    }

    @Override
    public int markAllAsRead(Long userId) {
        List<Long> ids = userNotifications.getOrDefault(userId, new ArrayList<>());
        int count = 0;
        for (Long id : ids) {
            NotificationRecord record = records.get(id);
            if (record != null && Integer.valueOf(0).equals(record.getReadStatus())) {
                record.setReadStatus(1);
                record.setReadTime(LocalDateTime.now());
                count++;
            }
        }
        log.info("全部标记已读: userId={}, count={}", userId, count);
        return count;
    }

    @Override
    public boolean deleteNotification(Long recordId) {
        NotificationRecord record = records.remove(recordId);
        if (record != null && record.getReceiverId() != null) {
            List<Long> ids = userNotifications.get(record.getReceiverId());
            if (ids != null) {
                ids.remove(recordId);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteNotifications(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        for (Long id : ids) {
            deleteNotification(id);
        }
        return true;
    }

    @Override
    public List<NotificationRecord> listAllRecords() {
        return new ArrayList<>(records.values());
    }

    // ==================== 重试与队列处理 ====================

    @Override
    public boolean retry(Long recordId) {
        NotificationRecord record = records.get(recordId);
        if (record == null) return false;
        
        record.setStatus(0);  // STATUS_PENDING
        record.setRetryCount(record.getRetryCount() != null ? record.getRetryCount() + 1 : 1);
        return true;
    }

    @Override
    public int processPendingNotifications() {
        int processed = 0;
        for (NotificationRecord record : records.values()) {
            if (Integer.valueOf(0).equals(record.getStatus())) {  // STATUS_PENDING
                try {
                    // 模拟发送
                    record.setStatus(2);  // STATUS_SUCCESS
                    record.setSendTime(LocalDateTime.now());
                    processed++;
                } catch (Exception e) {
                    record.setStatus(3);  // STATUS_FAILED
                    record.setFailReason(e.getMessage());
                }
            }
        }
        log.info("处理待发送通知: processed={}", processed);
        return processed;
    }

    // ==================== 辅助方法 ====================

    private void saveAndSend(NotificationRecord record) {
        records.put(record.getId(), record);
        if (record.getReceiverId() != null) {
            userNotifications.computeIfAbsent(record.getReceiverId(), k -> new ArrayList<>())
                    .add(record.getId());
        }
    }

    private Integer getReadStatusValue(NotificationRecord record) {
        return Integer.valueOf(1).equals(record.getReadStatus()) ? 1 : 0;
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
