package cn.aiedge.mq.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.mq.model.MessageEntity;
import cn.aiedge.mq.producer.EnhancedMessageProducer;
import cn.aiedge.mq.service.MessageQueueMonitor;
import cn.aiedge.mq.service.MessageRetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 增强版消息队列管理控制器
 * 提供更全面的消息队列管理、监控和运维功能
 * 
 * @author AI-Ready Team
 * @since 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/mq/enhanced")
@RequiredArgsConstructor
@Tag(name = "消息队列管理", description = "增强版消息队列管理功能")
@ConditionalOnProperty(prefix = "mq.rabbit", name = "enabled", havingValue = "true")
public class EnhancedMessageQueueController {

    private final EnhancedMessageProducer messageProducer;
    private final MessageQueueMonitor messageQueueMonitor;
    private final MessageRetryService retryService;

    // ==================== 消息发送增强 ====================

    @PostMapping("/send/batch")
    @Operation(summary = "批量发送消息")
    public Result<Object> sendBatchMessage(
            @RequestParam String routingKey,
            @RequestBody List<MessageEntity> messages) {
        
        var result = messageProducer.sendBatch(routingKey, messages);
        
        return Result.success(Map.of(
            "batchResult", result,
            "successRate", result.getSuccessRate(),
            "message", String.format("批量发送完成: 成功%d条，失败%d条", 
                result.getSuccessCount(), result.getFailedCount())
        ));
    }

    @PostMapping("/send/idempotent")
    @Operation(summary = "发送幂等性消息")
    public Result<Object> sendIdempotentMessage(
            @RequestParam String routingKey,
            @RequestParam String deduplicationId,
            @RequestBody MessageEntity message) {
        
        boolean success = messageProducer.sendIdempotent(routingKey, message, deduplicationId);
        
        return Result.success(Map.of(
            "success", success,
            "messageId", message.getMessageId(),
            "message", success ? "幂等性消息发送成功" : "消息已存在，未重复发送"
        ));
    }

    @PostMapping("/send/transaction")
    @Operation(summary = "发送事务消息")
    public Result<Object> sendTransactionMessage(
            @RequestParam String routingKey,
            @RequestBody MessageEntity message) {
        
        try {
            messageProducer.sendTransactionMessage(routingKey, message);
            return Result.success(Map.of(
                "messageId", message.getMessageId(),
                "message", "事务消息发送成功"
            ));
        } catch (Exception e) {
            log.error("事务消息发送失败: {}", message.getMessageId(), e);
            return Result.error("事务消息发送失败: " + e.getMessage());
        }
    }

    @PostMapping("/send/order-created")
    @Operation(summary = "发送订单创建消息")
    public Result<Object> sendOrderCreated(
            @RequestParam Long orderId,
            @RequestBody Object orderData) {
        
        try {
            messageProducer.sendOrderCreated(orderId, orderData);
            return Result.success(Map.of(
                "orderId", orderId,
                "message", "订单创建消息发送成功"
            ));
        } catch (Exception e) {
            log.error("订单创建消息发送失败: {}", orderId, e);
            return Result.error("订单创建消息发送失败: " + e.getMessage());
        }
    }

    @PostMapping("/send/payment-result")
    @Operation(summary = "发送支付结果消息")
    public Result<Object> sendPaymentResult(
            @RequestParam Long orderId,
            @RequestParam String paymentId,
            @RequestParam boolean success,
            @RequestParam String message) {
        
        try {
            messageProducer.sendPaymentResult(orderId, paymentId, success, message);
            return Result.success(Map.of(
                "orderId", orderId,
                "paymentId", paymentId,
                "success", success,
                "message", "支付结果消息发送成功"
            ));
        } catch (Exception e) {
            log.error("支付结果消息发送失败: {}", orderId, e);
            return Result.error("支付结果消息发送失败: " + e.getMessage());
        }
    }

    // ==================== 监控功能 ====================

    @GetMapping("/monitor/metrics")
    @Operation(summary = "获取监控指标")
    public Result<Object> getMetrics() {
        var metrics = messageQueueMonitor.getMetrics();
        return Result.success(metrics);
    }

    @GetMapping("/monitor/health")
    @Operation(summary = "健康检查")
    public Result<Object> healthCheck() {
        var healthStatus = messageQueueMonitor.checkHealth();
        return Result.success(healthStatus);
    }

    @GetMapping("/monitor/stats")
    @Operation(summary = "获取统计信息")
    public Result<Object> getStats() {
        var metrics = messageQueueMonitor.getMetrics();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSend", metrics.getTotalSend());
        stats.put("totalConsume", metrics.getTotalConsume());
        stats.put("totalErrors", metrics.getTotalErrors());
        stats.put("successRate", metrics.getTotalSend() > 0 ? 
            (double) (metrics.getTotalSend() - metrics.getTotalErrors()) / metrics.getTotalSend() : 1.0);
        
        return Result.success(stats);
    }

    // ==================== 重试管理 ====================

    @GetMapping("/retry/status/{messageId}")
    @Operation(summary = "获取消息重试状态")
    public Result<Object> getRetryStatus(@PathVariable String messageId) {
        var retryStatus = retryService.getRetryStatus(
            MessageEntity.builder().messageId(messageId).build()
        );
        return Result.success(retryStatus);
    }

    @PostMapping("/retry/resend/{messageId}")
    @Operation(summary = "重新发送失败的消息")
    public Result<Object> resendFailedMessage(
            @PathVariable String messageId,
            @RequestParam String routingKey) {
        
        var message = MessageEntity.builder()
            .messageId(messageId)
            .build();
            
        boolean success = messageProducer.resendMessage(routingKey, message);
        
        return Result.success(Map.of(
            "messageId", messageId,
            "success", success,
            "message", success ? "消息重新发送成功" : "消息重新发送失败"
        ));
    }

    @PostMapping("/retry/batch-resend")
    @Operation(summary = "批量重发消息")
    public Result<Object> batchResendMessages(
            @RequestParam String routingKey,
            @RequestBody List<MessageEntity> messages) {
        
        boolean result = messageProducer.resendBatch(routingKey, messages);
        int successCount = result ? messages.size() : 0;
        
        return Result.success(Map.of(
            "successCount", successCount,
            "totalCount", messages.size(),
            "message", String.format("批量重发完成: 成功%d条", successCount)
        ));
    }

    // ==================== 队列管理 ====================

    @GetMapping("/queues")
    @Operation(summary = "获取队列信息")
    public Result<Object> getQueueInfo() {
        Map<String, Object> queues = new HashMap<>();
        
        // 这里应该调用RabbitMQ Management API获取真实队列信息
        // 暂时返回模拟数据
        queues.put("email", Map.of("name", "ai.ready.email", "status", "running", "messages", 0));
        queues.put("sms", Map.of("name", "ai.ready.sms", "status", "running", "messages", 0));
        queues.put("notification", Map.of("name", "ai.ready.notification", "status", "running", "messages", 0));
        queues.put("order.created", Map.of("name", "ai.ready.order.created", "status", "running", "messages", 0));
        queues.put("payment.result", Map.of("name", "ai.ready.payment.result", "status", "running", "messages", 0));
        queues.put("stock.changed", Map.of("name", "ai.ready.stock.changed", "status", "running", "messages", 0));
        queues.put("dead.letter", Map.of("name", "ai.ready.dead.letter", "status", "running", "messages", 0));
        
        return Result.success(queues);
    }

    @GetMapping("/queues/{queueName}/status")
    @Operation(summary = "获取指定队列状态")
    public Result<Object> getQueueStatus(@PathVariable String queueName) {
        // 这里应该调用RabbitMQ Management API
        Map<String, Object> status = new HashMap<>();
        status.put("queueName", queueName);
        status.put("status", "running");
        status.put("messages", 0);
        status.put("consumers", 1);
        status.put("ready", 0);
        status.put("unacked", 0);
        
        return Result.success(status);
    }

    // ==================== 消息管理 ====================

    @GetMapping("/messages/pending")
    @Operation(summary = "获取待处理消息统计")
    public Result<Object> getPendingMessages() {
        // 这里应该从RabbitMQ Management API获取实际数据
        Map<String, Object> pending = new HashMap<>();
        pending.put("totalPending", 0);
        pending.put("byQueue", new HashMap<>());
        
        return Result.success(pending);
    }
}
