package cn.aiedge.ready.mq.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 消息队列监控指标服务
 * 提供Prometheus格式的监控指标
 */
@Service
public class MqMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(MqMetricsService.class);
    private static final String PREFIX = "aiedge_mq_";

    private final MeterRegistry meterRegistry;
    private final AmqpAdmin amqpAdmin;

    // 消息计数器
    private final Map<String, Counter> publishedCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> consumedCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> failedCounters = new ConcurrentHashMap<>();
    
    // 处理时间计时器
    private final Map<String, Timer> processingTimers = new ConcurrentHashMap<>();
    
    // 队列积压指标
    private final Map<String, Long> queueBacklog = new ConcurrentHashMap<>();

    @Autowired
    public MqMetricsService(MeterRegistry meterRegistry, AmqpAdmin amqpAdmin) {
        this.meterRegistry = meterRegistry;
        this.amqpAdmin = amqpAdmin;
    }

    @PostConstruct
    public void init() {
        logger.info("消息队列监控指标服务初始化完成");
        
        // 注册全局指标
        registerGlobalMetrics();
    }

    /**
     * 注册全局指标
     */
    private void registerGlobalMetrics() {
        // 队列积压总数
        Gauge.builder(PREFIX + "queue_backlog_total", queueBacklog, map -> 
                map.values().stream().mapToLong(Long::longValue).sum())
            .description("消息队列总积压数量")
            .register(meterRegistry);
    }

    /**
     * 记录消息发布
     */
    public void recordPublished(String queueName) {
        Counter counter = publishedCounters.computeIfAbsent(queueName, 
            name -> Counter.builder(PREFIX + "messages_published")
                .tag("queue", name)
                .description("已发布消息数量")
                .register(meterRegistry));
        counter.increment();
        logger.debug("记录消息发布: queue={}", queueName);
    }

    /**
     * 记录消息消费
     */
    public void recordConsumed(String queueName) {
        Counter counter = consumedCounters.computeIfAbsent(queueName, 
            name -> Counter.builder(PREFIX + "messages_consumed")
                .tag("queue", name)
                .description("已消费消息数量")
                .register(meterRegistry));
        counter.increment();
        logger.debug("记录消息消费: queue={}", queueName);
    }

    /**
     * 记录消息处理失败
     */
    public void recordFailed(String queueName, String errorType) {
        Counter counter = failedCounters.computeIfAbsent(queueName + "_" + errorType, 
            key -> Counter.builder(PREFIX + "messages_failed")
                .tag("queue", queueName)
                .tag("error_type", errorType)
                .description("处理失败消息数量")
                .register(meterRegistry));
        counter.increment();
        logger.warn("记录消息处理失败: queue={}, errorType={}", queueName, errorType);
    }

    /**
     * 记录消息处理时间
     */
    public void recordProcessingTime(String queueName, long durationMs) {
        Timer timer = processingTimers.computeIfAbsent(queueName, 
            name -> Timer.builder(PREFIX + "processing_time")
                .tag("queue", name)
                .description("消息处理时间")
                .register(meterRegistry));
        timer.record(durationMs, TimeUnit.MILLISECONDS);
        logger.debug("记录处理时间: queue={}, duration={}ms", queueName, durationMs);
    }

    /**
     * 更新队列积压数量
     */
    public void updateQueueBacklog(String queueName, long backlog) {
        queueBacklog.put(queueName, backlog);
        
        // 更新单个队列积压指标
        Gauge.builder(PREFIX + "queue_backlog", () -> backlog)
            .tag("queue", queueName)
            .description("队列积压消息数量")
            .register(meterRegistry);
            
        logger.debug("更新队列积压: queue={}, backlog={}", queueName, backlog);
    }

    /**
     * 获取队列信息
     */
    public Map<String, Object> getQueueInfo(String queueName) {
        Map<String, Object> info = new HashMap<>();
        try {
            Properties props = amqpAdmin.getQueueInfo(queueName);
            if (props != null) {
                info.put("name", queueName);
                info.put("messageCount", props.get("messageCount"));
                info.put("consumerCount", props.get("consumerCount"));
                
                // 更新积压指标
                Object messageCount = props.get("messageCount");
                if (messageCount instanceof Number) {
                    updateQueueBacklog(queueName, ((Number) messageCount).longValue());
                }
            }
        } catch (Exception e) {
            logger.error("获取队列信息失败: {}", queueName, e);
            info.put("error", e.getMessage());
        }
        return info;
    }

    /**
     * 获取所有队列状态
     */
    public Map<String, Map<String, Object>> getAllQueuesStatus() {
        Map<String, Map<String, Object>> status = new HashMap<>();
        try {
            // 获取所有队列名称（从已注册的计数器中提取）
            publishedCounters.keySet().forEach(queueName -> {
                status.put(queueName, getQueueInfo(queueName));
            });
        } catch (Exception e) {
            logger.error("获取所有队列状态失败", e);
        }
        return status;
    }
}
