package cn.aiedge.mq.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 消息队列监控服务
 * 提供消息发送、消费指标监控和健康检查
 * 
 * @author AI-Ready Team
 * @since 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mq.rabbit", name = "enabled", havingValue = "true")
public class MessageQueueMonitor {

    private final MeterRegistry meterRegistry;
    private final RabbitTemplate rabbitTemplate;
    
    private final ConcurrentHashMap<String, Long> sendCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> consumeCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> errorCounters = new ConcurrentHashMap<>();

    /**
     * 记录消息发送指标
     */
    public void recordSendMessage(String topic, long durationMs, boolean success) {
        // 记录发送耗时
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("mq.send.duration")
            .tag("topic", topic)
            .tag("success", String.valueOf(success))
            .register(meterRegistry));
        
        // 记录发送数量
        String sendKey = "mq.send.count." + topic;
        sendCounters.compute(topic, (k, v) -> (v == null) ? 1L : v + 1);
        
        Counter.builder("mq.send.total")
            .tag("topic", topic)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .increment();
        
        // 记录错误
        if (!success) {
            String errorKey = "mq.send.error." + topic;
            errorCounters.compute(topic, (k, v) -> (v == null) ? 1L : v + 1);
            
            Counter.builder("mq.send.errors")
                .tag("topic", topic)
                .register(meterRegistry)
                .increment();
        }
    }

    /**
     * 记录消息消费指标
     */
    public void recordConsumeMessage(String topic, long durationMs, boolean success) {
        // 记录消费耗时
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("mq.consume.duration")
            .tag("topic", topic)
            .tag("success", String.valueOf(success))
            .register(meterRegistry));
        
        // 记录消费数量
        String consumeKey = "mq.consume.count." + topic;
        consumeCounters.compute(topic, (k, v) -> (v == null) ? 1L : v + 1);
        
        Counter.builder("mq.consume.total")
            .tag("topic", topic)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .increment();
        
        // 记录错误
        if (!success) {
            String errorKey = "mq.consume.error." + topic;
            errorCounters.compute(topic, (k, v) -> (v == null) ? 1L : v + 1);
            
            Counter.builder("mq.consume.errors")
                .tag("topic", topic)
                .register(meterRegistry)
                .increment();
        }
    }

    /**
     * 获取监控指标摘要
     */
    public MonitorMetrics getMetrics() {
        MonitorMetrics metrics = new MonitorMetrics();
        
        metrics.setTotalSend(sendCounters.values().stream().mapToLong(Long::longValue).sum());
        metrics.setTotalConsume(consumeCounters.values().stream().mapToLong(Long::longValue).sum());
        metrics.setTotalErrors(errorCounters.values().stream().mapToLong(Long::longValue).sum());
        
        metrics.setSendCounters(new ConcurrentHashMap<>(sendCounters));
        metrics.setConsumeCounters(new ConcurrentHashMap<>(consumeCounters));
        metrics.setErrorCounters(new ConcurrentHashMap<>(errorCounters));
        
        return metrics;
    }

    /**
     * 检查队列健康状态
     */
    public QueueHealthStatus checkHealth() {
        QueueHealthStatus status = new QueueHealthStatus();
        
        try {
            // 检查RabbitMQ连接
            var connection = rabbitTemplate.getConnectionFactory().createConnection();
            try {
                connection.createChannel(false).close();
                status.setConnected(true);
                status.setStatus("UP");
            } finally {
                connection.close();
            }
            
            status.setCheckTime(System.currentTimeMillis());
            status.setMessage("RabbitMQ连接正常");
        } catch (Exception e) {
            status.setConnected(false);
            status.setStatus("DOWN");
            status.setCheckTime(System.currentTimeMillis());
            status.setMessage("RabbitMQ连接异常: " + e.getMessage());
        }
        
        return status;
    }

    /**
     * 监控指标
     */
    public static class MonitorMetrics {
        private long totalSend = 0;
        private long totalConsume = 0;
        private long totalErrors = 0;
        private ConcurrentHashMap<String, Long> sendCounters = new ConcurrentHashMap<>();
        private ConcurrentHashMap<String, Long> consumeCounters = new ConcurrentHashMap<>();
        private ConcurrentHashMap<String, Long> errorCounters = new ConcurrentHashMap<>();

        // Getters and setters
        public long getTotalSend() { return totalSend; }
        public void setTotalSend(long totalSend) { this.totalSend = totalSend; }
        
        public long getTotalConsume() { return totalConsume; }
        public void setTotalConsume(long totalConsume) { this.totalConsume = totalConsume; }
        
        public long getTotalErrors() { return totalErrors; }
        public void setTotalErrors(long totalErrors) { this.totalErrors = totalErrors; }
        
        public ConcurrentHashMap<String, Long> getSendCounters() { return sendCounters; }
        public void setSendCounters(ConcurrentHashMap<String, Long> sendCounters) { this.sendCounters = sendCounters; }
        
        public ConcurrentHashMap<String, Long> getConsumeCounters() { return consumeCounters; }
        public void setConsumeCounters(ConcurrentHashMap<String, Long> consumeCounters) { this.consumeCounters = consumeCounters; }
        
        public ConcurrentHashMap<String, Long> getErrorCounters() { return errorCounters; }
        public void setErrorCounters(ConcurrentHashMap<String, Long> errorCounters) { this.errorCounters = errorCounters; }
    }

    /**
     * 队列健康状态
     */
    public static class QueueHealthStatus {
        private boolean connected;
        private String status;
        private String message;
        private long checkTime;

        // Getters and setters
        public boolean isConnected() { return connected; }
        public void setConnected(boolean connected) { this.connected = connected; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public long getCheckTime() { return checkTime; }
        public void setCheckTime(long checkTime) { this.checkTime = checkTime; }
    }
}
