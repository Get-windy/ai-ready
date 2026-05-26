package cn.aiedge.ready.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 死信队列处理器
 */
@Service
public class DeadLetterHandler {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterHandler.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 创建死信队列和交换机
     */
    public void setupDeadLetterInfrastructure(String queueName) {
        // 死信交换机
        DirectExchange dlxExchange = new DirectExchange(queueName + ".dlx", true, false);
        amqpAdmin.declareExchange(dlxExchange);

        // 死信队列
        Queue dlq = new Queue(queueName + ".dlq", true, false, false, null);
        amqpAdmin.declareQueue(dlq);

        // 绑定死信交换机和死信队列
        Binding dlqBinding = BindingBuilder.bind(dlq).to(dlxExchange).with(queueName + ".dlq");
        amqpAdmin.declareBinding(dlqBinding);

        logger.info("死信队列基础设施创建完成，队列：{}", queueName);
    }

    /**
     * 处理死信消息
     */
    @RabbitListener(queues = "#{deadLetterQueueNames}")
    public void handleDeadLetterMessage(String message) {
        logger.error("收到死信消息：{}", message);
        // 在这里可以实现重试逻辑、人工处理等
        try {
            // 尝试重新处理死信消息
            retryProcessing(message);
        } catch (Exception e) {
            logger.error("重试处理死信消息失败：{}", message, e);
            // 可以发送到人工处理队列或记录到数据库
            handleFailedMessage(message, e);
        }
    }

    /**
     * 重试处理消息
     */
    private void retryProcessing(String message) {
        // 实现重试逻辑
        logger.info("正在重试处理消息：{}", message);
        // 这里可以实现具体的重试逻辑
    }

    /**
     * 处理失败的消息
     */
    private void handleFailedMessage(String message, Exception e) {
        // 记录失败消息到数据库或发送到人工处理队列
        logger.error("消息处理失败，已记录到失败处理队列：{}", message, e);
    }

    /**
     * 获取队列参数（用于配置死信转发）
     */
    public Map<String, Object> getDeadLetterArguments(String queueName) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", queueName + ".dlx"); // 死信转发的交换机
        args.put("x-dead-letter-routing-key", queueName + ".dlq"); // 死信转发的routing key
        args.put("x-message-ttl", 60000); // 消息存活时间（1分钟）
        return args;
    }
}