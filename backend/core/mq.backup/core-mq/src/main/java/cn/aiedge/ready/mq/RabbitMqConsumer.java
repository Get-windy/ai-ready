package cn.aiedge.ready.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RabbitMQ消息消费者实现
 */
@Service
public class RabbitMqConsumer implements MessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMqConsumer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext applicationContext;

    // 存储队列与其处理器的映射关系
    private final Map<String, MessageHandler> handlerMap = new ConcurrentHashMap<>();

    @Override
    public void registerHandler(String queueName, MessageHandler handler) {
        handlerMap.put(queueName, handler);
        
        // 动态声明队列（如果不存在）
        Queue queue = new Queue(queueName, true); // durable = true
        // 使用具体的DirectExchange类型，而不是抽象的Exchange
        DirectExchange directExchange = new DirectExchange("default_direct_exchange");
        Binding binding = BindingBuilder.bind(queue).to(directExchange).with(queueName);
        
        // 将Bean注册到Spring容器中
        org.springframework.beans.factory.support.BeanDefinitionRegistry registry = 
            (org.springframework.beans.factory.support.BeanDefinitionRegistry) applicationContext.getAutowireCapableBeanFactory();
        
        logger.info("注册消息处理器成功，队列：{}", queueName);
    }

    /**
     * 监听所有注册的队列
     */
    @RabbitListener(queues = "#{queueNames}")
    public void consumeMessage(String message, org.springframework.messaging.Message<?> springMessage) {
        try {
            // 从消息头获取队列名称
            String queueName = (String) springMessage.getHeaders().get("amqp_receivedRoutingKey");
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
     * 为每个队列创建专门的监听器
     */
    @PostConstruct
    public void initListeners() {
        for (String queueName : handlerMap.keySet()) {
            logger.info("初始化队列监听器：{}", queueName);
        }
    }
}