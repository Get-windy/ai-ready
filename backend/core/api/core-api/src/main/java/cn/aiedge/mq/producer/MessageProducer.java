package cn.aiedge.mq.producer;

import cn.aiedge.mq.config.RabbitMQConfig;
import cn.aiedge.mq.model.MessageEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 消息生产者
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mq.rabbit", name = "enabled", havingValue = "true")
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQConfig mqConfig;

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
     * 发送邮件消息
     */
    public void sendEmail(String to, String subject, String content) {
        MessageEntity message = MessageEntity.emailMessage(to, subject, content);
        send("email", message);
    }

    /**
     * 发送短信消息
     */
    public void sendSms(String phone, String content) {
        MessageEntity message = MessageEntity.smsMessage(phone, content);
        send("sms", message);
    }

    /**
     * 发送站内通知
     */
    public void sendNotification(Long userId, String title, String content) {
        MessageEntity message = MessageEntity.notificationMessage(userId, title, content);
        send("notification", message);
    }

    /**
     * 发送数据同步消息
     */
    public void sendDataSync(String dataType, Object data) {
        MessageEntity message = MessageEntity.of("sync", "DATA_SYNC", 
            new java.util.HashMap<String, Object>() {{
                put("dataType", dataType);
                put("data", data);
            }});
        send("sync", message);
    }

    /**
     * 批量发送消息
     */
    public void sendBatch(String routingKey, java.util.List<MessageEntity> messages) {
        for (MessageEntity message : messages) {
            send(routingKey, message);
        }
        log.info("批量消息发送完成: routingKey={}, count={}", routingKey, messages.size());
    }
}
