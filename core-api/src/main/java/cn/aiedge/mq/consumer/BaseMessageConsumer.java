package cn.aiedge.mq.consumer;

import cn.aiedge.mq.model.MessageEntity;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;

import java.io.IOException;
import java.util.Map;

/**
 * 消息消费者基类
 * 提供统一的消息处理和异常处理机制
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
public abstract class BaseMessageConsumer {

    /**
     * 处理消息（子类实现）
     *
     * @param message 消息实体
     * @return 是否处理成功
     */
    protected abstract boolean processMessage(MessageEntity message);

    /**
     * 获取消费者名称
     */
    protected String getConsumerName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 消息处理入口
     * 使用手动确认模式
     */
    protected void handleMessage(MessageEntity messageEntity, Message message, Channel channel) 
            throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = messageEntity.getMessageId();

        try {
            log.debug("[{}] 开始处理消息: messageId={}", getConsumerName(), messageId);

            boolean success = processMessage(messageEntity);

            if (success) {
                // 处理成功，确认消息
                channel.basicAck(deliveryTag, false);
                log.debug("[{}] 消息处理成功: messageId={}", getConsumerName(), messageId);
            } else {
                // 处理失败，检查是否需要重试
                handleFailure(messageEntity, message, channel, 
                    new RuntimeException("消息处理返回失败"));
            }

        } catch (Exception e) {
            log.error("[{}] 消息处理异常: messageId={}", getConsumerName(), messageId, e);
            handleFailure(messageEntity, message, channel, e);
        }
    }

    /**
     * 处理失败
     */
    protected void handleFailure(MessageEntity messageEntity, Message message, 
                                  Channel channel, Exception e) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        // 检查是否可以重试
        if (messageEntity.canRetry()) {
            // 拒绝消息，重新入队
            channel.basicNack(deliveryTag, false, true);
            log.warn("[{}] 消息处理失败，等待重试: messageId={}, retryCount={}", 
                getConsumerName(), messageEntity.getMessageId(), messageEntity.getRetryCount());
        } else {
            // 超过最大重试次数，拒绝消息（进入死信队列）
            channel.basicReject(deliveryTag, false);
            log.error("[{}] 消息处理失败，超过最大重试次数: messageId={}", 
                getConsumerName(), messageEntity.getMessageId());
            
            // 记录失败消息
            recordFailedMessage(messageEntity, e);
        }
    }

    /**
     * 记录失败消息
     */
    protected void recordFailedMessage(MessageEntity message, Exception e) {
        // 子类可覆盖此方法实现持久化存储
        log.error("[{}] 消息处理失败记录: messageId={}, error={}", 
            getConsumerName(), message.getMessageId(), e.getMessage());
    }

    /**
     * 消息预处理钩子
     */
    protected void beforeProcess(MessageEntity message) {
        // 子类可覆盖
    }

    /**
     * 消息后处理钩子
     */
    protected void afterProcess(MessageEntity message, boolean success, Exception error) {
        // 子类可覆盖
    }

    /**
     * 提取消息头信息
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> extractHeaders(Message message) {
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        return headers != null ? headers : Map.of();
    }
}
