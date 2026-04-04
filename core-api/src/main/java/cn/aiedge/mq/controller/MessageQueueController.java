package cn.aiedge.mq.controller;

import cn.aiedge.mq.model.MessageEntity;
import cn.aiedge.mq.producer.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息队列管理控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/mq")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mq.rabbit", name = "enabled", havingValue = "true")
public class MessageQueueController {

    private final MessageProducer messageProducer;
    private final RabbitTemplate rabbitTemplate;

    // ==================== 消息发送 ====================

    @PostMapping("/send/email")
    public ResponseEntity<Map<String, Object>> sendEmail(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String content) {
        
        messageProducer.sendEmail(to, subject, content);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "邮件消息已发送"
        ));
    }

    @PostMapping("/send/sms")
    public ResponseEntity<Map<String, Object>> sendSms(
            @RequestParam String phone,
            @RequestParam String content) {
        
        messageProducer.sendSms(phone, content);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "短信消息已发送"
        ));
    }

    @PostMapping("/send/notification")
    public ResponseEntity<Map<String, Object>> sendNotification(
            @RequestParam Long userId,
            @RequestParam String title,
            @RequestParam String content) {
        
        messageProducer.sendNotification(userId, title, content);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "通知消息已发送"
        ));
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestParam String routingKey,
            @RequestBody MessageEntity message) {
        
        messageProducer.send(routingKey, message);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "messageId", message.getMessageId(),
            "message", "消息已发送"
        ));
    }

    @PostMapping("/send-delayed")
    public ResponseEntity<Map<String, Object>> sendDelayedMessage(
            @RequestParam String routingKey,
            @RequestParam long delayMillis,
            @RequestBody MessageEntity message) {
        
        messageProducer.sendDelayed(routingKey, message, delayMillis);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "messageId", message.getMessageId(),
            "delayMs", delayMillis,
            "message", "延迟消息已发送"
        ));
    }

    // ==================== 队列状态 ====================

    @GetMapping("/queues")
    public ResponseEntity<Map<String, Object>> getQueueInfo() {
        Map<String, Object> queues = new HashMap<>();
        
        // 获取队列信息（需要管理插件）
        try {
            // 这里简化处理，实际应该调用RabbitMQ Management API
            queues.put("email", Map.of("name", "ai.ready.email", "status", "running"));
            queues.put("sms", Map.of("name", "ai.ready.sms", "status", "running"));
            queues.put("notification", Map.of("name", "ai.ready.notification", "status", "running"));
            queues.put("dataSync", Map.of("name", "ai.ready.data.sync", "status", "running"));
            queues.put("deadLetter", Map.of("name", "ai.ready.dead.letter", "status", "running"));
        } catch (Exception e) {
            log.warn("获取队列信息失败: {}", e.getMessage());
        }
        
        return ResponseEntity.ok(queues);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // 检查RabbitMQ连接
            rabbitTemplate.getConnectionFactory().createConnection().close();
            health.put("status", "UP");
            health.put("connected", true);
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("connected", false);
            health.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(health);
    }

    // ==================== 消息统计 ====================

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        // 实际应该从RabbitMQ Management API获取
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSent", 0);
        stats.put("totalConsumed", 0);
        stats.put("totalFailed", 0);
        stats.put("pendingMessages", 0);
        
        return ResponseEntity.ok(stats);
    }
}
