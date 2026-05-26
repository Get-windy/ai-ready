package cn.aiedge.ready.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 消息队列自动配置类
 */
@Configuration
@EnableRabbit
@ConditionalOnProperty(prefix = "aiedge.mq", name = "type", havingValue = "rabbitmq", matchIfMissing = true)
public class MqAutoConfiguration {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 配置RabbitTemplate，使用JSON消息转换器
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter(objectMapper));
        return template;
    }

    /**
     * 配置监听器工厂，使用JSON消息转换器
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter(objectMapper));
        return factory;
    }

    /**
     * 消息生产者Bean
     */
    @Bean
    public MessageProducer messageProducer() {
        return new RabbitMqProducer();
    }

    /**
     * 消息消费者Bean
     */
    @Bean
    public MessageConsumer messageConsumer() {
        return new EnhancedMessageConsumer();
    }

    /**
     * 死信处理器Bean
     */
    @Bean
    public DeadLetterHandler deadLetterHandler(AmqpAdmin amqpAdmin, RabbitTemplate rabbitTemplate) {
        DeadLetterHandler handler = new DeadLetterHandler();
        return handler;
    }
}