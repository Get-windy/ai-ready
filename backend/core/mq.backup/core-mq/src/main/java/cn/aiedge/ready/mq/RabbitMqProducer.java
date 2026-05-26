package cn.aiedge.ready.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ消息生产者实现
 */
@Service
public class RabbitMqProducer implements MessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMqProducer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void sendMessage(String queueName, Object message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            CorrelationData correlationId = new CorrelationData(queueName + "_" + System.currentTimeMillis());

            // Spring AMQP convertAndSend需要正确的参数顺序：exchange, routingKey, message, correlationData
            rabbitTemplate.convertAndSend("", queueName, jsonMessage, correlationId);
            logger.info("消息发送成功，队列：{}，消息ID：{}", queueName, correlationId.getId());
        } catch (JsonProcessingException e) {
            logger.error("序列化消息失败", e);
            throw new RuntimeException("序列化消息失败", e);
        } catch (AmqpException e) {
            logger.error("发送消息到队列失败：{}", queueName, e);
            throw new RuntimeException("发送消息失败", e);
        }
    }

    @Override
    public void sendDelayedMessage(String queueName, Object message, long delay) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            // 设置延迟时间（毫秒转换为微秒传递给RabbitMQ的x-delayed-message插件）
            messageProperties.setHeader("x-delay", delay);
            
            Message rabbitMessage = new Message(jsonMessage.getBytes(), messageProperties);
            CorrelationData correlationId = new CorrelationData(queueName + "_" + System.currentTimeMillis());

            // 发送到延迟交换机
            rabbitTemplate.send("delayed_exchange", queueName, rabbitMessage, correlationId);
            logger.info("延迟消息发送成功，队列：{}，延迟：{}ms，消息ID：{}", queueName, delay, correlationId.getId());
        } catch (JsonProcessingException e) {
            logger.error("序列化消息失败", e);
            throw new RuntimeException("序列化消息失败", e);
        } catch (AmqpException e) {
            logger.error("发送延迟消息到队列失败：{}", queueName, e);
            throw new RuntimeException("发送延迟消息失败", e);
        }
    }

    @Override
    public void sendToExchange(String exchange, String routingKey, Object message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            CorrelationData correlationId = new CorrelationData(exchange + "_" + routingKey + "_" + System.currentTimeMillis());

            rabbitTemplate.convertAndSend(exchange, routingKey, jsonMessage, correlationId);
            logger.info("消息发送到交换机成功，交换机：{}，路由键：{}，消息ID：{}", exchange, routingKey, correlationId.getId());
        } catch (JsonProcessingException e) {
            logger.error("序列化消息失败", e);
            throw new RuntimeException("序列化消息失败", e);
        } catch (AmqpException e) {
            logger.error("发送消息到交换机失败：{}，路由键：{}", exchange, routingKey, e);
            throw new RuntimeException("发送消息到交换机失败", e);
        }
    }
}