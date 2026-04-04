package cn.aiedge.mq.config;

import lombok.Data;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ配置
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mq.rabbit")
@ConditionalOnProperty(prefix = "mq.rabbit", name = "enabled", havingValue = "true")
public class RabbitMQConfig {

    /**
     * 是否启用
     */
    private boolean enabled = false;

    /**
     * 默认交换机名称
     */
    private String defaultExchange = "ai-ready-exchange";

    /**
     * 默认队列前缀
     */
    private String queuePrefix = "ai.ready.";

    /**
     * 消费者并发数
     */
    private int concurrentConsumers = 3;

    /**
     * 最大消费者数
     */
    private int maxConcurrentConsumers = 10;

    /**
     * 预取数量
     */
    private int prefetchCount = 10;

    /**
     * 消息转换器（JSON）
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                // 消息发送失败处理
                System.err.println("消息发送失败: " + cause);
            }
        });
        template.setReturnsCallback(returned -> {
            System.err.println("消息被退回: " + returned.getMessage());
        });
        return template;
    }

    /**
     * 监听容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setConcurrentConsumers(concurrentConsumers);
        factory.setMaxConcurrentConsumers(maxConcurrentConsumers);
        factory.setPrefetchCount(prefetchCount);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }

    /**
     * 默认交换机
     */
    @Bean
    public DirectExchange defaultExchange() {
        return new DirectExchange(defaultExchange, true, false);
    }

    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(defaultExchange + ".dlx", true, false);
    }

    // ==================== 预定义队列 ====================

    /**
     * 系统通知队列
     */
    @Bean
    public Queue notificationQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", defaultExchange + ".dlx");
        args.put("x-dead-letter-routing-key", "notification.dead");
        args.put("x-message-ttl", 86400000); // 24小时过期
        return new Queue(queuePrefix + "notification", true, false, false, args);
    }

    /**
     * 邮件发送队列
     */
    @Bean
    public Queue emailQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", defaultExchange + ".dlx");
        args.put("x-dead-letter-routing-key", "email.dead");
        return new Queue(queuePrefix + "email", true, false, false, args);
    }

    /**
     * 短信发送队列
     */
    @Bean
    public Queue smsQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", defaultExchange + ".dlx");
        args.put("x-dead-letter-routing-key", "sms.dead");
        return new Queue(queuePrefix + "sms", true, false, false, args);
    }

    /**
     * 数据同步队列
     */
    @Bean
    public Queue dataSyncQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", defaultExchange + ".dlx");
        args.put("x-dead-letter-routing-key", "sync.dead");
        return new Queue(queuePrefix + "data.sync", true, false, false, args);
    }

    /**
     * 死信队列
     */
    @Bean
    public Queue deadLetterQueue() {
        return new Queue(queuePrefix + "dead.letter", true);
    }

    // ==================== 绑定 ====================

    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange defaultExchange) {
        return BindingBuilder.bind(notificationQueue).to(defaultExchange).with("notification");
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, DirectExchange defaultExchange) {
        return BindingBuilder.bind(emailQueue).to(defaultExchange).with("email");
    }

    @Bean
    public Binding smsBinding(Queue smsQueue, DirectExchange defaultExchange) {
        return BindingBuilder.bind(smsQueue).to(defaultExchange).with("sms");
    }

    @Bean
    public Binding dataSyncBinding(Queue dataSyncQueue, DirectExchange defaultExchange) {
        return BindingBuilder.bind(dataSyncQueue).to(defaultExchange).with("sync");
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with("dead");
    }
}
