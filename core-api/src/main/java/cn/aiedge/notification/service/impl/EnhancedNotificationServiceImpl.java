package cn.aiedge.notification.service.impl;

import cn.aiedge.notification.cache.NotificationTemplateCache;
import cn.aiedge.notification.channel.NotificationChannel;
import cn.aiedge.notification.config.NotificationProperties;
import cn.aiedge.notification.entity.NotificationRecord;
import cn.aiedge.notification.entity.NotificationTemplate;
import cn.aiedge.notification.limiter.NotificationRateLimiter;
import cn.aiedge.notification.service.NotificationSendRequest;
import cn.aiedge.notification.service.NotificationService;
import cn.aiedge.notification.template.TemplateRenderer;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 增强版通知服务实现
 * 
 * 新增特性：
 * - 异步发送支持
 * - 模板缓存
 * - 发送限流
 * - 优先级队列
 * - 定时发送
 * - 批量发送优化
 * 
 * @author AI-Ready Team
 * @since 1.1.0
 */
@Slf4j
@Service("enhancedNotificationService")
@RequiredArgsConstructor
public class EnhancedNotificationServiceImpl implements NotificationService {

    private final TemplateRepository templateRepository;
    private final NotificationRecordRepository recordRepository;
    private final TemplateRenderer templateRenderer;
    private final List<NotificationChannel> channels;
    private final NotificationTemplateCache templateCache;
    private final NotificationRateLimiter rateLimiter;
    private final NotificationProperties properties;
    private final ThreadPoolTaskExecutor notificationExecutor;

    /**
     * 待发送队列（优先级队列）
     */
    private final PriorityBlockingQueue<ScheduledNotification> scheduledQueue = new PriorityBlockingQueue<>();

    // ==================== 模板管理（使用缓存） ====================

    @Override
    @Transactional
    public NotificationTemplate createTemplate(NotificationTemplate template) {
        if (template.getStatus() == null) {
            template.setStatus(NotificationTemplate.STATUS_ENABLED);
        }
        template = templateRepository.save(template);
        templateCache.updateTemplate(template);
        return template;
    }

    @Override
    @Transactional
    public NotificationTemplate updateTemplate(NotificationTemplate template) {
        template = templateRepository.save(template);
        templateCache.updateTemplate(template);
        return template;
    }

    @Override
    @Transactional
    public boolean deleteTemplate(Long templateId) {
        templateRepository.deleteById(templateId);
        templateCache.removeTemplate(templateId);
        return true;
    }

    @Override
    public NotificationTemplate getTemplate(Long templateId) {
        return templateCache.getById(templateId);
    }

    @Override
    public NotificationTemplate getTemplateByCode(String templateCode) {
        return templateCache.getByCode(templateCode);
    }

    @Override
    public List<NotificationTemplate> getAllTemplates() {
        return templateCache.getAllEnabled();
    }

    @Override
    public List<NotificationTemplate> getTemplatesByType(String notifyType) {
        return templateCache.getAllEnabled().stream()
                .filter(t -> notifyType.equals(t.getNotifyType()))
                .toList();
    }

    // ==================== 增强的通知发送 ====================

    /**
     * 增强版发送（支持优先级和定时）
     */
    public NotificationRecord sendEnhanced(NotificationSendRequest request) {
        // 处理直接发送（不使用模板）
        if (request.getTemplateCode().startsWith("__direct_")) {
            return sendDirect(request);
        }

        NotificationTemplate template = getTemplateByCode(request.getTemplateCode());
        if (template == null) {
            throw new IllegalArgumentException("模板不存在: " + request.getTemplateCode());
        }

        // 渲染模板
        String title = templateRenderer.render(template.getTitle(), request.getVariables());
        String content = templateRenderer.render(template.getContent(), request.getVariables());

        // 创建记录
        NotificationRecord record = buildRecord(template, request, title, content);

        // 检查是否定时发送
        if (request.getScheduledTime() != null && request.getScheduledTime().isAfter(LocalDateTime.now())) {
            record.setStatus(NotificationRecord.STATUS_PENDING);
            record = recordRepository.save(record);
            addToScheduledQueue(record, request.getScheduledTime(), request.getPriority());
            log.info("定时通知已加入队列: recordId={}, scheduledTime={}", record.getId(), request.getScheduledTime());
            return record;
        }

        record = recordRepository.save(record);

        // 检查是否异步发送
        if (request.isAsync() && properties.isAsyncEnabled()) {
            doSendAsync(record, request.getPriority());
        } else {
            doSend(record);
        }

        return record;
    }

    private NotificationRecord sendDirect(NotificationSendRequest request) {
        Map<String, Object> vars = request.getVariables();
        if (vars == null) {
            vars = new HashMap<>();
            request.setVariables(vars);
        }

        String title = String.valueOf(vars.getOrDefault("__title__", ""));
        String content = String.valueOf(vars.getOrDefault("__content__", ""));
        String notifyType = request.getTemplateCode().equals("__direct_email__") 
                ? NotificationTemplate.TYPE_EMAIL 
                : NotificationTemplate.TYPE_SITE;

        NotificationRecord record = NotificationRecord.builder()
                .notifyType(notifyType)
                .receiverId(request.getReceiverId())
                .receiverType(request.getReceiverType())
                .receiverAddress(request.getReceiverAddress())
                .title(title)
                .content(content)
                .status(NotificationRecord.STATUS_PENDING)
                .readStatus(NotificationRecord.READ_UNREAD)
                .retryCount(0)
                .bizType(request.getBizType())
                .bizId(request.getBizId())
                .createTime(LocalDateTime.now())
                .build();

        record = recordRepository.save(record);

        if (request.isAsync() && properties.isAsyncEnabled()) {
            doSendAsync(record, request.getPriority());
        } else {
            doSend(record);
        }

        return record;
    }

    @Override
    @Transactional
    public NotificationRecord send(String templateCode, Long receiverId, String receiverType,
                                   String receiverAddress, Map<String, Object> variables) {
        NotificationSendRequest request = NotificationSendRequest.of(templateCode, receiverId, receiverType, variables);
        request.setReceiverAddress(receiverAddress);
        return sendEnhanced(request);
    }

    @Override
    public NotificationRecord sendSiteMessage(Long receiverId, String title, String content) {
        NotificationSendRequest request = NotificationSendRequest.ofSiteMessage(receiverId, title, content);
        return sendEnhanced(request);
    }

    @Override
    public NotificationRecord sendEmail(String email, String title, String content) {
        NotificationSendRequest request = NotificationSendRequest.ofEmail(email, title, content);
        return sendEnhanced(request);
    }

    @Override
    public NotificationRecord sendSms(String phone, String content) {
        NotificationRecord record = NotificationRecord.builder()
                .notifyType(NotificationTemplate.TYPE_SMS)
                .receiverAddress(phone)
                .content(content)
                .status(NotificationRecord.STATUS_PENDING)
                .retryCount(0)
                .createTime(LocalDateTime.now())
                .build();

        record = recordRepository.save(record);
        doSendAsync(record, 5);
        return record;
    }

    // ==================== 批量发送优化 ====================

    @Override
    @Transactional
    public List<NotificationRecord> sendBatch(String templateCode, List<Long> receiverIds,
                                               String receiverType, Map<String, Object> variables) {
        NotificationTemplate template = getTemplateByCode(templateCode);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在: " + templateCode);
        }

        // 预渲染模板（避免重复渲染）
        String title = templateRenderer.render(template.getTitle(), variables);
        String content = templateRenderer.render(template.getContent(), variables);

        // 批量创建记录
        List<NotificationRecord> records = new ArrayList<>();
        int batchSize = properties.getBatchSize();
        
        for (int i = 0; i < receiverIds.size(); i++) {
            Long receiverId = receiverIds.get(i);
            
            NotificationRecord record = NotificationRecord.builder()
                    .templateId(template.getId())
                    .templateCode(templateCode)
                    .notifyType(template.getNotifyType())
                    .receiverId(receiverId)
                    .receiverType(receiverType)
                    .title(title)
                    .content(content)
                    .status(NotificationRecord.STATUS_PENDING)
                    .readStatus(NotificationRecord.READ_UNREAD)
                    .retryCount(0)
                    .createTime(LocalDateTime.now())
                    .build();
            
            records.add(record);
            
            // 分批保存
            if (records.size() >= batchSize || i == receiverIds.size() - 1) {
                records = recordRepository.saveAll(records);
                
                // 异步发送
                for (NotificationRecord r : records) {
                    doSendAsync(r, 5);
                }
                
                records.clear();
            }
        }

        log.info("批量发送完成: templateCode={}, count={}", templateCode, receiverIds.size());
        return recordRepository.findByTemplateCodeOrderByCreateTimeDesc(templateCode, receiverIds.size());
    }

    /**
     * 批量发送到不同接收者（每个接收者可以使用不同的变量）
     */
    @Transactional
    public List<NotificationRecord> sendBatchPersonalized(String templateCode, 
            List<BatchReceiverInfo> receivers) {
        
        NotificationTemplate template = getTemplateByCode(templateCode);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在: " + templateCode);
        }

        List<NotificationRecord> allRecords = new ArrayList<>();
        List<NotificationRecord> batch = new ArrayList<>();

        for (BatchReceiverInfo receiver : receivers) {
            // 每个接收者单独渲染
            String title = templateRenderer.render(template.getTitle(), receiver.variables());
            String content = templateRenderer.render(template.getContent(), receiver.variables());

            NotificationRecord record = NotificationRecord.builder()
                    .templateId(template.getId())
                    .templateCode(templateCode)
                    .notifyType(template.getNotifyType())
                    .receiverId(receiver.receiverId())
                    .receiverType(receiver.receiverType())
                    .receiverAddress(receiver.receiverAddress())
                    .title(title)
                    .content(content)
                    .status(NotificationRecord.STATUS_PENDING)
                    .readStatus(NotificationRecord.READ_UNREAD)
                    .retryCount(0)
                    .createTime(LocalDateTime.now())
                    .build();

            batch.add(record);

            if (batch.size() >= properties.getBatchSize()) {
                batch = recordRepository.saveAll(batch);
                allRecords.addAll(batch);
                batch.forEach(r -> doSendAsync(r, 5));
                batch.clear();
            }
        }

        // 处理剩余记录
        if (!batch.isEmpty()) {
            batch = recordRepository.saveAll(batch);
            allRecords.addAll(batch);
            batch.forEach(r -> doSendAsync(r, 5));
        }

        return allRecords;
    }

    // ==================== 通知记录管理 ====================

    @Override
    public List<NotificationRecord> getUserNotifications(Long userId, Integer readStatus, int limit) {
        if (readStatus == null) {
            return recordRepository.findByReceiverIdOrderByCreateTimeDesc(userId, limit);
        }
        return recordRepository.findByReceiverIdAndReadStatusOrderByCreateTimeDesc(userId, readStatus, limit);
    }

    @Override
    public int getUnreadCount(Long userId) {
        return recordRepository.countByReceiverIdAndReadStatus(userId, NotificationRecord.READ_UNREAD);
    }

    @Override
    @Transactional
    public boolean markAsRead(Long recordId) {
        NotificationRecord record = recordRepository.findById(recordId).orElse(null);
        if (record != null) {
            record.setReadStatus(NotificationRecord.READ_READ);
            record.setReadTime(LocalDateTime.now());
            recordRepository.save(record);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public int markAllAsRead(Long userId) {
        return recordRepository.updateReadStatusByReceiverId(userId, 
                NotificationRecord.READ_READ, LocalDateTime.now());
    }

    @Override
    @Transactional
    public boolean deleteNotification(Long recordId) {
        recordRepository.deleteById(recordId);
        return true;
    }

    // ==================== 重试与队列处理 ====================

    @Override
    @Transactional
    public boolean retry(Long recordId) {
        NotificationRecord record = recordRepository.findById(recordId).orElse(null);
        if (record == null || record.getStatus() != NotificationRecord.STATUS_FAILED) {
            return false;
        }
        
        // 检查重试次数
        if (record.getRetryCount() >= properties.getMaxRetryCount()) {
            log.warn("超过最大重试次数: recordId={}, retryCount={}", recordId, record.getRetryCount());
            return false;
        }

        record.setStatus(NotificationRecord.STATUS_PENDING);
        recordRepository.save(record);
        doSendAsync(record, 3);
        return true;
    }

    @Override
    @Transactional
    public int processPendingNotifications() {
        List<NotificationRecord> pending = recordRepository.findByStatus(NotificationRecord.STATUS_PENDING);
        int count = 0;
        for (NotificationRecord record : pending) {
            doSend(record);
            count++;
        }
        return count;
    }

    /**
     * 处理定时发送队列
     */
    public int processScheduledQueue() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledNotification> toSend = new ArrayList<>();
        
        ScheduledNotification item;
        while ((item = scheduledQueue.peek()) != null && !item.scheduledTime.isAfter(now)) {
            scheduledQueue.poll();
            toSend.add(item);
        }

        for (ScheduledNotification sn : toSend) {
            NotificationRecord record = recordRepository.findById(sn.recordId).orElse(null);
            if (record != null && record.getStatus() == NotificationRecord.STATUS_PENDING) {
                doSendAsync(record, sn.priority);
            }
        }

        return toSend.size();
    }

    // ==================== 私有方法 ====================

    private NotificationRecord buildRecord(NotificationTemplate template, 
            NotificationSendRequest request, String title, String content) {
        return NotificationRecord.builder()
                .templateId(template.getId())
                .templateCode(request.getTemplateCode())
                .notifyType(template.getNotifyType())
                .receiverId(request.getReceiverId())
                .receiverType(request.getReceiverType())
                .receiverAddress(request.getReceiverAddress())
                .title(title)
                .content(content)
                .status(NotificationRecord.STATUS_PENDING)
                .readStatus(NotificationRecord.READ_UNREAD)
                .retryCount(0)
                .bizType(request.getBizType())
                .bizId(request.getBizId())
                .createTime(LocalDateTime.now())
                .build();
    }

    @Async("notificationExecutor")
    protected void doSendAsync(NotificationRecord record, Integer priority) {
        doSend(record);
    }

    protected void doSend(NotificationRecord record) {
        // 限流检查
        if (!rateLimiter.tryAcquire(record.getNotifyType())) {
            long waitMs = rateLimiter.getWaitTimeMs(record.getNotifyType());
            log.info("触发限流，等待{}ms后重试: notifyType={}", waitMs, record.getNotifyType());
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        NotificationChannel channel = getChannel(record.getNotifyType());
        if (channel == null) {
            updateRecordFailed(record, "未找到对应的通知渠道");
            return;
        }

        try {
            record.setStatus(NotificationRecord.STATUS_SENDING);
            recordRepository.save(record);

            NotificationChannel.SendResult result = channel.send(record);

            if (result.success()) {
                record.setStatus(NotificationRecord.STATUS_SUCCESS);
                record.setSendTime(LocalDateTime.now());
            } else {
                handleSendFailure(record, result.message());
            }

            recordRepository.save(record);

        } catch (Exception e) {
            log.error("通知发送异常: recordId={}", record.getId(), e);
            handleSendFailure(record, "发送异常: " + e.getMessage());
            recordRepository.save(record);
        }
    }

    private void handleSendFailure(NotificationRecord record, String reason) {
        record.setStatus(NotificationRecord.STATUS_FAILED);
        record.setFailReason(reason);
        record.setRetryCount(record.getRetryCount() + 1);
    }

    private void updateRecordFailed(NotificationRecord record, String reason) {
        record.setStatus(NotificationRecord.STATUS_FAILED);
        record.setFailReason(reason);
        recordRepository.save(record);
    }

    private NotificationChannel getChannel(String notifyType) {
        return channels.stream()
                .filter(c -> c.getChannelType().equals(notifyType) && c.isAvailable())
                .findFirst()
                .orElse(null);
    }

    private void addToScheduledQueue(NotificationRecord record, LocalDateTime scheduledTime, Integer priority) {
        scheduledQueue.offer(new ScheduledNotification(record.getId(), scheduledTime, priority != null ? priority : 5));
    }

    // ==================== 内部类 ====================

    private record ScheduledNotification(Long recordId, LocalDateTime scheduledTime, int priority) 
            implements Comparable<ScheduledNotification> {
        @Override
        public int compareTo(ScheduledNotification other) {
            // 优先级高的排前面，时间早的排前面
            int priorityCompare = Integer.compare(other.priority, this.priority);
            if (priorityCompare != 0) return priorityCompare;
            return this.scheduledTime.compareTo(other.scheduledTime);
        }
    }

    public record BatchReceiverInfo(Long receiverId, String receiverType, String receiverAddress, 
                                     Map<String, Object> variables) {}

    // Repository接口
    public interface TemplateRepository extends org.springframework.data.jpa.repository.JpaRepository<NotificationTemplate, Long> {
        Optional<NotificationTemplate> findByTemplateCode(String templateCode);
        List<NotificationTemplate> findByNotifyType(String notifyType);
    }

    public interface NotificationRecordRepository extends org.springframework.data.jpa.repository.JpaRepository<NotificationRecord, Long> {
        List<NotificationRecord> findByReceiverIdOrderByCreateTimeDesc(Long receiverId, int limit);
        List<NotificationRecord> findByReceiverIdAndReadStatusOrderByCreateTimeDesc(Long receiverId, Integer readStatus, int limit);
        int countByReceiverIdAndReadStatus(Long receiverId, Integer readStatus);
        int updateReadStatusByReceiverId(Long receiverId, Integer readStatus, LocalDateTime readTime);
        List<NotificationRecord> findByStatus(Integer status);
        List<NotificationRecord> findByTemplateCodeOrderByCreateTimeDesc(String templateCode, int limit);
        List<NotificationRecord> saveAll(List<NotificationRecord> records);
    }
}
