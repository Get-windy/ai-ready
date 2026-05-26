package cn.aiedge.mq.producer;

import cn.aiedge.mq.config.RabbitMQConfig;
import cn.aiedge.mq.model.MessageEntity;
import cn.aiedge.mq.service.MessageRetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 增强版消息生产者
 * 提供批量发送、事务消息、异步发送等高级功能
 * 
 * @author AI-Ready Team
 * @since 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mq.rabbit", name = "enabled", havingValue = "true")
public class EnhancedMessageProducer {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQConfig mqConfig;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MessageRetryService retryService;

    /**
     * 发送消息到默认交换机
     *
     * @param routingKey 路由键
     * @param message    消息
     */
    public void send(String routingKey, MessageEntity message) {
        send(mqConfig.getDefaultExchange(), routingKey, message);
    }

    /**
     * 发送消息到指定交换机
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息
     */
    public void send(String exchange, String routingKey, MessageEntity message) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, message, msg -> {
                // 持久化消息
                msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                // 设置消息ID
                msg.getMessageProperties().setMessageId(message.getMessageId());
                // 设置类型
                msg.getMessageProperties().setType(message.getMessageType());
                // 设置过期时间
                if (message.getExpireTime() != null) {
                    long ttl = message.getExpireTime() - Instant.now().toEpochMilli();
                    if (ttl > 0) {
                        msg.getMessageProperties().setExpiration(String.valueOf(ttl));
                    }
                }
                return msg;
            });

            log.debug("消息发送成功: exchange={}, routingKey={}, messageId={}", 
                exchange, routingKey, message.getMessageId());

        } catch (Exception e) {
            log.error("消息发送失败: exchange={}, routingKey={}, messageId={}", 
                exchange, routingKey, message.getMessageId(), e);
            throw new RuntimeException("消息发送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 异步发送消息
     */
    public CompletableFuture<Boolean> sendAsync(String routingKey, MessageEntity message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                send(routingKey, message);
                return true;
            } catch (Exception e) {
                log.error("异步发送消息失败: {}", message.getMessageId(), e);
                return false;
            }
        });
    }

    /**
     * 发送幂等性消息（防止重复发送）
     */
    public boolean sendIdempotent(String routingKey, MessageEntity message, String deduplicationId) {
        String dedupKey = "mq:duplicate:" + deduplicationId;
        
        // 检查是否已存在
        if (redisTemplate.hasKey(dedupKey)) {
            log.warn("消息重复发送: dedupId={}", deduplicationId);
            return false;
        }
        
        // 设置去重标记（有效期1小时）
        redisTemplate.opsForValue().set(dedupKey, message.getMessageId(), 1, TimeUnit.HOURS);
        
        // 发送消息
        send(routingKey, message);
        return true;
    }

    /**
     * 批量发送消息
     */
    public BatchSendResult sendBatch(String routingKey, List<MessageEntity> messages) {
        int successCount = 0;
        List<String> failedMessages = new ArrayList<>();
        
        for (MessageEntity message : messages) {
            try {
                send(routingKey, message);
                successCount++;
            } catch (Exception e) {
                failedMessages.add(message.getMessageId());
                log.error("批量发送消息失败: {}", message.getMessageId(), e);
            }
        }
        
        return new BatchSendResult(successCount, messages.size(), failedMessages);
    }

    /**
     * 发送延迟消息
     *
     * @param routingKey  路由键
     * @param message     消息
     * @param delayMillis 延迟毫秒数
     */
    public void sendDelayed(String routingKey, MessageEntity message, long delayMillis) {
        try {
            rabbitTemplate.convertAndSend(
                mqConfig.getDefaultExchange(),
                routingKey,
                message,
                msg -> {
                    msg.getMessageProperties().setDelay((int) delayMillis);
                    msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return msg;
                });

            log.debug("延迟消息发送成功: routingKey={}, delay={}ms, messageId={}", 
                routingKey, delayMillis, message.getMessageId());

        } catch (Exception e) {
            log.error("延迟消息发送失败: routingKey={}, messageId={}", 
                routingKey, message.getMessageId(), e);
            throw new RuntimeException("延迟消息发送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送事务消息（伪事务，实际是补偿机制）
     */
    public void sendTransactionMessage(String routingKey, MessageEntity message) {
        // 这里可以结合本地事务实现最终一致性
        // 1. 保存消息到本地待发送表
        // 2. 执行业务逻辑
        // 3. 如果业务成功，发送消息；如果失败，记录补偿任务
        send(routingKey, message);
    }

    /**
     * 发送订单创建消息
     */
    public void sendOrderCreated(Long orderId, Object orderData) {
        MessageEntity message = MessageEntity.of("order.created", "ORDER_CREATED", 
            new java.util.HashMap<String, Object>() {{
                put("orderId", orderId);
                put("orderData", orderData);
                put("timestamp", Instant.now().toEpochMilli());
            }});
        send("order.created", message);
    }

    /**
     * 发送支付结果消息
     */
    public void sendPaymentResult(Long orderId, String paymentId, boolean success, String messageDetail) {
        MessageEntity message = MessageEntity.of("payment.result", "PAYMENT_RESULT", 
            new java.util.HashMap<String, Object>() {{
                put("orderId", orderId);
                put("paymentId", paymentId);
                put("success", success);
                put("message", messageDetail);
                put("timestamp", Instant.now().toEpochMilli());
            }});
        send("payment.result", message);
    }

    /**
     * 发送库存变动消息
     */
    public void sendStockChanged(Long productId, Integer quantity, String changeType, Object detail) {
        MessageEntity message = MessageEntity.of("stock.changed", "STOCK_CHANGED", 
            new java.util.HashMap<String, Object>() {{
                put("productId", productId);
                put("quantity", quantity);
                put("changeType", changeType);
                put("detail", detail);
                put("timestamp", Instant.now().toEpochMilli());
            }});
        send("stock.changed", message);
    }

    /**
     * 批量发送结果
     */
    public static class BatchSendResult {
        private final int successCount;
        private final int totalCount;
        private final List<String> failedMessages;

        public BatchSendResult(int successCount, int totalCount, List<String> failedMessages) {
            this.successCount = successCount;
            this.totalCount = totalCount;
            this.failedMessages = failedMessages;
        }

        public int getSuccessCount() { return successCount; }
        public int getTotalCount() { return totalCount; }
        public int getFailedCount() { return totalCount - successCount; }
        public List<String> getFailedMessages() { return failedMessages; }
        public double getSuccessRate() { return totalCount > 0 ? (double) successCount / totalCount : 0; }

        @Override
        public String toString() {
            return String.format("BatchSendResult{success=%d, total=%d, failed=%d, successRate=%.2f%%}", 
                successCount, totalCount, getFailedCount(), getSuccessRate() * 100);
        }
    }

    /**
     * 重新发送单个消息（用于消息重试）
     *
     * @param messageId 消息ID
     * @param entity    消息实体
     */
    public boolean resendMessage(String messageId, MessageEntity entity) {
        try {
            // 直接发送消息（重试逻辑由消费者处理）
            send(entity.getTopic(), entity);
            log.info("消息重试发送成功: messageId={}", messageId);
            return true;
        } catch (Exception e) {
            log.error("消息重试发送失败: messageId={}, error={}", messageId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 批量重新发送消息
     *
     * @param messageId 消息ID
     * @param entities  消息实体列表
     */
    public boolean resendBatch(String messageId, List<MessageEntity> entities) {
        try {
            for (MessageEntity entity : entities) {
                send(entity.getTopic(), entity);
            }
            log.info("批量消息重试发送成功: messageId={}, count={}", messageId, entities.size());
            return true;
        } catch (Exception e) {
            log.error("批量消息重试发送失败: messageId={}, count={}, error={}", 
                messageId, entities.size(), e.getMessage(), e);
            return false;
        }
    }
}
