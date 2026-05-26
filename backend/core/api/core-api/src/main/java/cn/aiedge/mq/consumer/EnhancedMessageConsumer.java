package cn.aiedge.mq.consumer;

import cn.aiedge.mq.config.RabbitMQConfig;
import cn.aiedge.mq.model.MessageEntity;
import cn.aiedge.mq.service.MessageRetryService;
import com.google.common.util.concurrent.RateLimiter;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * 增强版消息消费者处理器
 * 支持限流、异步处理、重试等高级功能
 * 
 * @author AI-Ready Team
 * @since 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mq.rabbit", name = "enabled", havingValue = "true")
public class EnhancedMessageConsumer {

    private final RabbitMQConfig mqConfig;
    private final MessageRetryService retryService;
    
    // 限流器：每秒最多100个请求
    private final RateLimiter rateLimiter = RateLimiter.create(100.0);

    // ==================== 订单消息消费者 ====================

    /**
     * 监听订单创建消息
     */
    @RabbitListener(queues = "#{@rabbitMQConfig.queuePrefix + 'order.created'}")
    public void handleOrderCreated(MessageEntity message, Message rawMessage, Channel channel) 
            throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        
        try {
            // 限流控制
            if (!rateLimiter.tryAcquire()) {
                // 消费速率超限，重新入队
                channel.basicNack(deliveryTag, false, true);
                log.warn("订单创建消息消费速率超限，重新入队: messageId={}", message.getMessageId());
                return;
            }

            log.info("处理订单创建消息: messageId={}", message.getMessageId());
            
            // TODO: 实际订单处理逻辑
            // orderService.handleOrderCreated(message.getPayload());
            
            channel.basicAck(deliveryTag, false);
            log.debug("订单创建消息处理完成: messageId={}", message.getMessageId());

        } catch (Exception e) {
            log.error("订单创建消息处理失败: messageId={}", message.getMessageId(), e);
            handleFailure(message, rawMessage, channel, e);
        }
    }

    /**
     * 监听支付结果消息
     */
    @RabbitListener(queues = "#{@rabbitMQConfig.queuePrefix + 'payment.result'}")
    public void handlePaymentResult(MessageEntity message, Message rawMessage, Channel channel) 
            throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        
        try {
            log.info("处理支付结果消息: messageId={}, orderId={}", 
                message.getMessageId(), getOrderIdFromPayload(message));

            // TODO: 实际支付结果处理逻辑
            // paymentService.handlePaymentResult(message.getPayload());
            
            channel.basicAck(deliveryTag, false);
            log.debug("支付结果消息处理完成: messageId={}", message.getMessageId());

        } catch (Exception e) {
            log.error("支付结果消息处理失败: messageId={}", message.getMessageId(), e);
            handleFailure(message, rawMessage, channel, e);
        }
    }

    /**
     * 监听库存变动消息
     */
    @RabbitListener(queues = "#{@rabbitMQConfig.queuePrefix + 'stock.changed'}")
    public void handleStockChanged(MessageEntity message, Message rawMessage, Channel channel) 
            throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        
        try {
            log.info("处理库存变动消息: messageId={}, productId={}", 
                message.getMessageId(), getProductIdFromPayload(message));

            // TODO: 实际库存变动处理逻辑
            // stockService.handleStockChange(message.getPayload());
            
            channel.basicAck(deliveryTag, false);
            log.debug("库存变动消息处理完成: messageId={}", message.getMessageId());

        } catch (Exception e) {
            log.error("库存变动消息处理失败: messageId={}", message.getMessageId(), e);
            handleFailure(message, rawMessage, channel, e);
        }
    }

    /**
     * 监听审批流程消息
     */
    @RabbitListener(queues = "#{@rabbitMQConfig.queuePrefix + 'approval'}")
    public void handleApprovalMessage(MessageEntity message, Message rawMessage, Channel channel) 
            throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        
        try {
            log.info("处理审批流程消息: messageId={}, type={}", 
                message.getMessageId(), message.getMessageType());

            // TODO: 实际审批流程处理逻辑
            // approvalService.handleApproval(message.getPayload());
            
            channel.basicAck(deliveryTag, false);
            log.debug("审批流程消息处理完成: messageId={}", message.getMessageId());

        } catch (Exception e) {
            log.error("审批流程消息处理失败: messageId={}", message.getMessageId(), e);
            handleFailure(message, rawMessage, channel, e);
        }
    }

    // ==================== 业务事件消息消费者 ====================

    /**
     * 监听用户登录事件
     */
    @RabbitListener(queues = "#{@rabbitMQConfig.queuePrefix + 'event.user.login'}")
    public void handleUserLoginEvent(MessageEntity message, Message rawMessage, Channel channel) 
            throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        
        try {
            log.info("处理用户登录事件: userId={}", getUserIdFromPayload(message));

            // TODO: 实际用户登录事件处理逻辑
            // eventService.handleUserLogin(message.getPayload());
            
            channel.basicAck(deliveryTag, false);
            log.debug("用户登录事件处理完成: messageId={}", message.getMessageId());

        } catch (Exception e) {
            log.error("用户登录事件处理失败: messageId={}", message.getMessageId(), e);
            handleFailure(message, rawMessage, channel, e);
        }
    }

    /**
     * 监听订单状态变更事件
     */
    @RabbitListener(queues = "#{@rabbitMQConfig.queuePrefix + 'event.order.status'}")
    public void handleOrderStatusChangeEvent(MessageEntity message, Message rawMessage, Channel channel) 
            throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        
        try {
            log.info("处理订单状态变更事件: orderId={}", getOrderIdFromPayload(message));

            // TODO: 实际订单状态变更处理逻辑
            // eventService.handleOrderStatusChange(message.getPayload());
            
            channel.basicAck(deliveryTag, false);
            log.debug("订单状态变更事件处理完成: messageId={}", message.getMessageId());

        } catch (Exception e) {
            log.error("订单状态变更事件处理失败: messageId={}", message.getMessageId(), e);
            handleFailure(message, rawMessage, channel, e);
        }
    }

    // ==================== 异步处理消费者 ====================

    /**
     * 异步处理邮件消息（高并发场景）
     */
    @RabbitListener(queues = "#{@rabbitMQConfig.queuePrefix + 'email'}", 
                    concurrency = "5-10") // 并发5-10个消费者
    public void handleEmailMessageAsync(MessageEntity message, Message rawMessage, Channel channel) 
            throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        
        try {
            // 使用异步处理避免阻塞
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("异步处理邮件消息: to={}", getEmailToFromPayload(message));
                    
                    // TODO: 实际邮件发送逻辑
                    // emailService.sendEmail(message.getPayload());
                    
                    // 手动确认消息
                    try {
                        channel.basicAck(deliveryTag, false);
                        log.debug("邮件消息异步处理完成: messageId={}", message.getMessageId());
                    } catch (IOException e) {
                        log.error("确认邮件消息失败: messageId={}", message.getMessageId(), e);
                    }
                } catch (Exception e) {
                    log.error("异步处理邮件消息失败: messageId={}", message.getMessageId(), e);
                    try {
                        handleFailure(message, rawMessage, channel, e);
                    } catch (IOException ioException) {
                        log.error("处理邮件消息失败确认失败: messageId={}", message.getMessageId(), ioException);
                    }
                }
            });
            
        } catch (Exception e) {
            log.error("邮件消息异步处理启动失败: messageId={}", message.getMessageId(), e);
            handleFailure(message, rawMessage, channel, e);
        }
    }

    // ==================== 辅助方法 ====================

    private Long getOrderIdFromPayload(MessageEntity message) {
        if (message.getPayload() instanceof java.util.Map) {
            java.util.Map<String, Object> payload = (java.util.Map<String, Object>) message.getPayload();
            Object orderId = payload.get("orderId");
            return orderId instanceof Number ? ((Number) orderId).longValue() : null;
        }
        return null;
    }

    private Long getProductIdFromPayload(MessageEntity message) {
        if (message.getPayload() instanceof java.util.Map) {
            java.util.Map<String, Object> payload = (java.util.Map<String, Object>) message.getPayload();
            Object productId = payload.get("productId");
            return productId instanceof Number ? ((Number) productId).longValue() : null;
        }
        return null;
    }

    private Long getUserIdFromPayload(MessageEntity message) {
        if (message.getPayload() instanceof java.util.Map) {
            java.util.Map<String, Object> payload = (java.util.Map<String, Object>) message.getPayload();
            Object userId = payload.get("userId");
            return userId instanceof Number ? ((Number) userId).longValue() : null;
        }
        return null;
    }

    private String getEmailToFromPayload(MessageEntity message) {
        if (message.getPayload() instanceof java.util.Map) {
            java.util.Map<String, Object> payload = (java.util.Map<String, Object>) message.getPayload();
            return (String) payload.get("to");
        }
        return null;
    }

    private void handleFailure(MessageEntity message, Message rawMessage, 
                               Channel channel, Exception e) throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        
        // 检查是否可以重试
        if (retryService.canRetry(message)) {
            // 增加重试次数
            int retryCount = retryService.incrementRetryCount(message.getMessageId());
            
            // 计算重试延迟
            long delay = retryService.calculateRetryDelay(message);
            
            log.warn("消息处理失败，等待重试: messageId={}, retryCount={}, delayMs={}", 
                message.getMessageId(), retryCount, delay);
                
            // 拒绝并重新入队
            channel.basicNack(deliveryTag, false, true);
        } else {
            // 超过最大重试次数，进入死信队列
            channel.basicReject(deliveryTag, false);
            log.error("消息处理失败，超过最大重试次数: messageId={}", message.getMessageId());
        }
    }
}
