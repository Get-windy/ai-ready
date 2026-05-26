package cn.aiedge.mq.service;

import cn.aiedge.mq.config.RabbitMQConfig;
import cn.aiedge.mq.model.MessageEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * 消息队列管理服务
 * 提供消息发送状态追踪、监控和管理能力
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mq.rabbit", name = "enabled", havingValue = "true")
public class MessageQueueService {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQConfig mqConfig;
    private final MessageRetryService retryService;

    /**
     * 重新发送失败的消息（手动重试）
     *
     * @param routingKey 路由键
     * @param message    消息实体
     * @return 是否发送成功
     */
    public boolean resendMessage(String routingKey, MessageEntity message) {
        try {
            // 清除重试计数
            retryService.clearRetryCount(message.getMessageId());
            
            // 重新发送
            rabbitTemplate.convertAndSend(
                mqConfig.getDefaultExchange(),
                routingKey,
                message,
                msg -> {
                    msg.getMessageProperties().setMessageId(message.getMessageId());
                    msg.getMessageProperties().setType(message.getMessageType());
                    return msg;
                });
            
            log.info("消息重发成功: messageId={}, routingKey={}", 
                message.getMessageId(), routingKey);
            return true;
            
        } catch (Exception e) {
            log.error("消息重发失败: messageId={}", message.getMessageId(), e);
            return false;
        }
    }

    /**
     * 批量重新发送消息
     *
     * @param routingKey 路由键
     * @param messages   消息列表
     * @return 成功数量
     */
    public int resendBatch(String routingKey, List<MessageEntity> messages) {
        int successCount = 0;
        for (MessageEntity message : messages) {
            if (resendMessage(routingKey, message)) {
                successCount++;
            }
        }
        log.info("批量重发完成: total={}, success={}", messages.size(), successCount);
        return successCount;
    }

    /**
     * 检查消息队列健康状态
     *
     * @return 健康状态信息
     */
    public QueueHealthStatus checkHealth() {
        QueueHealthStatus status = new QueueHealthStatus();
        
        try {
            // 检查RabbitMQ连接
            var connectionFactory = rabbitTemplate.getConnectionFactory();
            try (var connection = connectionFactory.createConnection()) {
                status.setConnected(true);
                status.setConnectionInfo(connection.toString());
            }
        } catch (Exception e) {
            status.setConnected(false);
            status.setErrorMessage(e.getMessage());
        }
        
        return status;
    }

    /**
     * 队列健康状态
     */
    @lombok.Data
    public static class QueueHealthStatus {
        private boolean connected;
        private String connectionInfo;
        private String errorMessage;
        private long checkTime = Instant.now().toEpochMilli();
    }
}
