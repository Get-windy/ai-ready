package cn.aiedge.mq.consumer;

import cn.aiedge.mq.config.RabbitMQConfig;
import cn.aiedge.mq.model.MessageEntity;
import cn.aiedge.mq.model.MessageEntity.EmailPayload;
import cn.aiedge.mq.model.MessageEntity.SmsPayload;
import cn.aiedge.mq.model.MessageEntity.NotificationPayload;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 消息消费者示例
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mq.rabbit", name = "enabled", havingValue = "true")
public class MessageConsumerHandler {

    // ==================== 邮件消费者 ====================

    /**
     * 监听邮件队列
     */
    @RabbitListener(queues = "#{rabbitMQConfig.queuePrefix + 'email'}")
    public void handleEmailMessage(MessageEntity message, Message rawMessage, Channel channel) 
            throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        
        try {
            EmailPayload email = convertPayload(message.getPayload(), EmailPayload.class);
            if (email == null) {
                log.warn("无效的邮件消息格式: messageId={}", message.getMessageId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            log.info("处理邮件消息: to={}, subject={}", email.getTo(), email.getSubject());
            
            // TODO: 实际发送邮件逻辑
            // emailService.send(email.getTo(), email.getSubject(), email.getContent());
            
            channel.basicAck(deliveryTag, false);
            log.debug("邮件消息处理完成: messageId={}", message.getMessageId());

        } catch (Exception e) {
            log.error("邮件消息处理失败: messageId={}", message.getMessageId(), e);
            handleFailure(message, rawMessage, channel, e);
        }
    }

    // ==================== 短信消费者 ====================

    /**
     * 监听短信队列
     */
    @RabbitListener(queues = "#{rabbitMQConfig.queuePrefix + 'sms'}")
    public void handleSmsMessage(MessageEntity message, Message rawMessage, Channel channel) 
            throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        
        try {
            SmsPayload sms = convertPayload(message.getPayload(), SmsPayload.class);
            if (sms == null) {
                log.warn("无效的短信消息格式: messageId={}", message.getMessageId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            log.info("处理短信消息: phone={}", sms.getPhone());
            
            // TODO: 实际发送短信逻辑
            // smsService.send(sms.getPhone(), sms.getContent());
            
            channel.basicAck(deliveryTag, false);
            log.debug("短信消息处理完成: messageId={}", message.getMessageId());

        } catch (Exception e) {
            log.error("短信消息处理失败: messageId={}", message.getMessageId(), e);
            handleFailure(message, rawMessage, channel, e);
        }
    }

    // ==================== 通知消费者 ====================

    /**
     * 监听通知队列
     */
    @RabbitListener(queues = "#{rabbitMQConfig.queuePrefix + 'notification'}")
    public void handleNotificationMessage(MessageEntity message, Message rawMessage, Channel channel) 
            throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        
        try {
            NotificationPayload notification = convertPayload(message.getPayload(), NotificationPayload.class);
            if (notification == null) {
                log.warn("无效的通知消息格式: messageId={}", message.getMessageId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            log.info("处理通知消息: userId={}, title={}", notification.getUserId(), notification.getTitle());
            
            // TODO: 实际发送通知逻辑
            // notificationService.send(notification.getUserId(), notification.getTitle(), notification.getContent());
            
            channel.basicAck(deliveryTag, false);
            log.debug("通知消息处理完成: messageId={}", message.getMessageId());

        } catch (Exception e) {
            log.error("通知消息处理失败: messageId={}", message.getMessageId(), e);
            handleFailure(message, rawMessage, channel, e);
        }
    }

    // ==================== 数据同步消费者 ====================

    /**
     * 监听数据同步队列
     */
    @RabbitListener(queues = "#{rabbitMQConfig.queuePrefix + 'data.sync'}")
    public void handleDataSyncMessage(MessageEntity message, Message rawMessage, Channel channel) 
            throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> syncData = convertPayload(message.getPayload(), java.util.Map.class);
            if (syncData == null) {
                log.warn("无效的同步数据格式: messageId={}", message.getMessageId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            String dataType = (String) syncData.get("dataType");
            Object data = syncData.get("data");
            
            log.info("处理数据同步消息: dataType={}", dataType);
            
            // TODO: 实际数据同步逻辑
            // dataSyncService.sync(dataType, data);
            
            channel.basicAck(deliveryTag, false);
            log.debug("数据同步消息处理完成: messageId={}", message.getMessageId());

        } catch (Exception e) {
            log.error("数据同步消息处理失败: messageId={}", message.getMessageId(), e);
            handleFailure(message, rawMessage, channel, e);
        }
    }

    // ==================== 死信队列消费者 ====================

    /**
     * 监听死信队列
     */
    @RabbitListener(queues = "#{rabbitMQConfig.queuePrefix + 'dead.letter'}")
    public void handleDeadLetterMessage(MessageEntity message, Message rawMessage, Channel channel) 
            throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        
        log.error("收到死信消息: messageId={}, topic={}", message.getMessageId(), message.getTopic());
        
        // 记录死信消息（可存储到数据库供后续人工处理）
        // deadLetterService.record(message);
        
        // 确认死信消息
        channel.basicAck(deliveryTag, false);
    }

    // ==================== 辅助方法 ====================

    @SuppressWarnings("unchecked")
    private <T> T convertPayload(Object payload, Class<T> targetClass) {
        if (payload == null) return null;
        
        if (targetClass.isInstance(payload)) {
            return (T) payload;
        }
        
        if (payload instanceof java.util.Map) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = 
                    new com.fasterxml.jackson.databind.ObjectMapper();
                String json = mapper.writeValueAsString(payload);
                return mapper.readValue(json, targetClass);
            } catch (Exception e) {
                log.warn("载荷转换失败: {}", e.getMessage());
                return null;
            }
        }
        
        return null;
    }

    private void handleFailure(MessageEntity message, Message rawMessage, 
                               Channel channel, Exception e) throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        
        if (message.canRetry()) {
            // 拒绝并重新入队
            channel.basicNack(deliveryTag, false, true);
            message.incrementRetry();
            log.warn("消息处理失败，等待重试: messageId={}, retryCount={}", 
                message.getMessageId(), message.getRetryCount());
        } else {
            // 超过最大重试次数，进入死信队列
            channel.basicReject(deliveryTag, false);
            log.error("消息处理失败，超过最大重试次数: messageId={}", message.getMessageId());
        }
    }
}
