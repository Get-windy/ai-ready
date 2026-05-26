package cn.aiedge.ready.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 增强版消息消费者实现，支持多种消息模式
 */
@Service
public class EnhancedMessageConsumer implements MessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedMessageConsumer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    // 存储队列与其处理器的映射关系
    private final Map<String, MessageHandler> handlerMap = new ConcurrentHashMap<>();

    @Override
    public void registerHandler(String queueName, MessageHandler handler) {
        handlerMap.put(queueName, handler);

        // 声明队列
        Queue queue = new Queue(queueName, true, false, false, null);
        amqpAdmin.declareQueue(queue);

        logger.info("注册消息处理器成功，队列：{}", queueName);
    }

    /**
     * 为特定队列创建监听方法
     */
    public void createQueueListener(String queueName) {
        logger.info("为队列创建监听器：{}", queueName);
    }

    /**
     * 通用消息监听器 - 需要为每个队列单独定义
     */
    @RabbitListener(queues = "default_queue")
    public void consumeDefaultMessage(String message) {
        handleMessage("default_queue", message);
    }

    /**
     * 处理消息的通用方法
     */
    private void handleMessage(String queueName, String message) {
        try {
            MessageHandler handler = handlerMap.get(queueName);

            if (handler != null) {
                logger.info("收到消息，队列：{}，消息内容：{}", queueName, message);
                handler.handleMessage(message);
            } else {
                logger.warn("未找到队列 {} 的处理器", queueName);
            }
        } catch (Exception e) {
            logger.error("处理消息时发生错误", e);
        }
    }

    /**
     * 订阅模式消息监听器
     */
    @RabbitListener(queues = "subscription_queue")
    public void consumeSubscriptionMessage(String message) {
        logger.info("收到订阅模式消息：{}", message);
        // 对所有订阅者广播消息
        for (Map.Entry<String, MessageHandler> entry : handlerMap.entrySet()) {
            try {
                entry.getValue().handleMessage(message);
            } catch (Exception e) {
                logger.error("处理订阅消息时发生错误，处理器：{}", entry.getKey(), e);
            }
        }
    }

    @PostConstruct
    public void init() {
        logger.info("增强版消息消费者初始化完成");
    }
}
