package cn.aiedge.ready.mq.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息队列积压告警服务
 */
@Service
public class MqBacklogAlertService {

    private static final Logger logger = LoggerFactory.getLogger(MqBacklogAlertService.class);

    @Value("${aiedge.mq.alert.backlog-threshold:100}")
    private long backlogThreshold;

    @Value("${aiedge.mq.alert.enabled:true}")
    private boolean alertEnabled;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired(required = false)
    private MqMetricsService metricsService;

    // 队列积压缓存
    private final Map<String, Long> backlogCache = new ConcurrentHashMap<>();
    
    // 已告警队列记录
    private final Map<String, Long> alertHistory = new ConcurrentHashMap<>();
    
    // 告警冷却时间（毫秒）
    private static final long ALERT_COOLDOWN_MS = 5 * 60 * 1000; // 5分钟

    @PostConstruct
    public void init() {
        logger.info("消息队列积压告警服务初始化完成，积压阈值: {}", backlogThreshold);
    }

    /**
     * 定时检查队列积压
     */
    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public void checkBacklog() {
        if (!alertEnabled) {
            return;
        }

        try {
            // 这里需要根据实际队列名称进行监控
            // 实际使用时应该从配置或自动发现获取队列列表
            String[] monitoredQueues = getMonitoredQueues();
            
            for (String queueName : monitoredQueues) {
                checkQueueBacklog(queueName);
            }
        } catch (Exception e) {
            logger.error("检查队列积压失败", e);
        }
    }

    /**
     * 检查单个队列积压
     */
    private void checkQueueBacklog(String queueName) {
        try {
            Properties props = amqpAdmin.getQueueInfo(queueName);
            if (props == null) {
                return;
            }

            Object messageCountObj = props.get("messageCount");
            long messageCount = 0;
            if (messageCountObj instanceof Number) {
                messageCount = ((Number) messageCountObj).longValue();
            }

            // 更新缓存
            backlogCache.put(queueName, messageCount);
            
            // 更新指标服务
            if (metricsService != null) {
                metricsService.updateQueueBacklog(queueName, messageCount);
            }

            // 检查是否超过阈值
            if (messageCount > backlogThreshold) {
                handleBacklogAlert(queueName, messageCount);
            } else {
                // 积压恢复正常，清除告警记录
                alertHistory.remove(queueName);
                if (messageCount == 0 && backlogCache.getOrDefault(queueName, 0L) > 0) {
                    logger.info("队列 {} 积压已清空", queueName);
                }
            }
        } catch (Exception e) {
            logger.error("获取队列 {} 信息失败", queueName, e);
        }
    }

    /**
     * 处理积压告警
     */
    private void handleBacklogAlert(String queueName, long backlog) {
        Long lastAlertTime = alertHistory.get(queueName);
        long now = System.currentTimeMillis();
        
        // 检查是否在冷却期内
        if (lastAlertTime != null && (now - lastAlertTime) < ALERT_COOLDOWN_MS) {
            logger.debug("队列 {} 积压告警在冷却期内，跳过", queueName);
            return;
        }

        // 记录告警
        alertHistory.put(queueName, now);
        
        // 发送告警日志
        logger.warn("⚠️ 消息队列积压告警! 队列: {}, 积压数量: {}, 阈值: {}", 
            queueName, backlog, backlogThreshold);
        
        // 这里可以扩展为发送告警通知（邮件、钉钉、飞书等）
        sendAlertNotification(queueName, backlog);
    }

    /**
     * 发送告警通知
     */
    private void sendAlertNotification(String queueName, long backlog) {
        // TODO: 实现告警通知逻辑
        // 可以集成钉钉、飞书、邮件等告警渠道
        logger.warn("告警通知: 队列 {} 积压 {} 条消息", queueName, backlog);
    }

    /**
     * 获取需要监控的队列列表
     * TODO: 从配置或自动发现获取
     */
    private String[] getMonitoredQueues() {
        // 这里返回常见队列，实际使用时应该从配置获取
        return new String[]{
            "transaction.queue",
            "notification.queue", 
            "async.task.queue",
            "email.queue"
        };
    }

    /**
     * 获取当前积压状态
     */
    public Map<String, Long> getBacklogStatus() {
        return new ConcurrentHashMap<>(backlogCache);
    }

    /**
     * 手动触发检查
     */
    public void manualCheck(String queueName) {
        checkQueueBacklog(queueName);
    }
}
