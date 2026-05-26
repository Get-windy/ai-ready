package cn.aiedge.mq.config;

import cn.aiedge.mq.consumer.EnhancedMessageConsumer;
import cn.aiedge.mq.producer.EnhancedMessageProducer;
import cn.aiedge.mq.service.MessageQueueMonitor;
import cn.aiedge.mq.service.MessageRetryService;
import lombok.Data;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 增强版消息队列配置
 * 包含更多预定义队列和增强功能配置
 * 
 * @author AI-Ready Team
 * @since 2.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mq.rabbit.enhanced")
@ConditionalOnProperty(prefix = "mq.rabbit", name = "enabled", havingValue = "true")
public class EnhancedRabbitMQConfig {

    /**
     * 是否启用增强功能
     */
    private boolean enabled = true;

    /**
     * 消息去重时间窗口（小时）
     */
    private int deduplicationWindowHours = 1;

    /**
     * 批量发送最大数量
     */
    private int maxBatchSize = 100;

    /**
     * 异步处理线程池大小
     */
    private int asyncProcessorThreads = 10;

    /**
     * 消费者限流配置
     */
    private ConsumerRateLimit consumerRateLimit = new ConsumerRateLimit();

    /**
     * 队列TTL配置
     */
    private QueueTtlConfig queueTtlConfig = new QueueTtlConfig();

    @Data
    public static class ConsumerRateLimit {
        /**
         * 每秒最大处理消息数
         */
        private double permitsPerSecond = 100.0;
    }

    @Data
    public static class QueueTtlConfig {
        /**
         * 邮件队列TTL（毫秒）
         */
        private long emailTtl = 86400000; // 24小时
        
        /**
         * 短信队列TTL（毫秒）
         */
        private long smsTtl = 43200000; // 12小时
        
        /**
         * 订单队列TTL（毫秒）
         */
        private long orderTtl = 604800000; // 7天
        
        /**
         * 支付队列TTL（毫秒）
         */
        private long paymentTtl = 86400000; // 24小时
        
        /**
         * 通知队列TTL（毫秒）
         */
        private long notificationTtl = 172800000; // 48小时
    }

    /**
     * 消息转换器（JSON）
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 监听容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory enhancedRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setConcurrentConsumers(5); // 增加并发消费者
        factory.setMaxConcurrentConsumers(20); // 最大并发消费者
        factory.setPrefetchCount(50); // 增加预取数量
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        return factory;
    }

    // ==================== 增强队列定义 ====================

    /**
     * 订单创建队列
     */
    @Bean
    public Queue orderCreatedQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "ai-ready-exchange.dlx");
        args.put("x-dead-letter-routing-key", "order.created.dead");
        args.put("x-message-ttl", getQueueTtlConfig().getOrderTtl());
        return new Queue("ai.ready.order.created", true, false, false, args);
    }

    /**
     * 支付结果队列
     */
    @Bean
    public Queue paymentResultQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "ai-ready-exchange.dlx");
        args.put("x-dead-letter-routing-key", "payment.result.dead");
        args.put("x-message-ttl", getQueueTtlConfig().getPaymentTtl());
        return new Queue("ai.ready.payment.result", true, false, false, args);
    }

    /**
     * 库存变动队列
     */
    @Bean
    public Queue stockChangedQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "ai-ready-exchange.dlx");
        args.put("x-dead-letter-routing-key", "stock.changed.dead");
        args.put("x-message-ttl", getQueueTtlConfig().getOrderTtl()); // 使用订单TTL
        return new Queue("ai.ready.stock.changed", true, false, false, args);
    }

    /**
     * 审批流程队列
     */
    @Bean
    public Queue approvalQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "ai-ready-exchange.dlx");
        args.put("x-dead-letter-routing-key", "approval.dead");
        args.put("x-message-ttl", getQueueTtlConfig().getNotificationTtl());
        return new Queue("ai.ready.approval", true, false, false, args);
    }

    /**
     * 用户登录事件队列
     */
    @Bean
    public Queue userLoginEventQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "ai-ready-exchange.dlx");
        args.put("x-dead-letter-routing-key", "event.user.login.dead");
        args.put("x-message-ttl", getQueueTtlConfig().getNotificationTtl());
        return new Queue("ai.ready.event.user.login", true, false, false, args);
    }

    /**
     * 订单状态变更事件队列
     */
    @Bean
    public Queue orderStatusChangeEventQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "ai-ready-exchange.dlx");
        args.put("x-dead-letter-routing-key", "event.order.status.dead");
        args.put("x-message-ttl", getQueueTtlConfig().getOrderTtl());
        return new Queue("ai.ready.event.order.status", true, false, false, args);
    }

    // ==================== 绑定 ====================

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, DirectExchange defaultExchange) {
        return org.springframework.amqp.core.BindingBuilder.bind(orderCreatedQueue).to(defaultExchange).with("order.created");
    }

    @Bean
    public Binding paymentResultBinding(Queue paymentResultQueue, DirectExchange defaultExchange) {
        return org.springframework.amqp.core.BindingBuilder.bind(paymentResultQueue).to(defaultExchange).with("payment.result");
    }

    @Bean
    public Binding stockChangedBinding(Queue stockChangedQueue, DirectExchange defaultExchange) {
        return org.springframework.amqp.core.BindingBuilder.bind(stockChangedQueue).to(defaultExchange).with("stock.changed");
    }

    @Bean
    public Binding approvalBinding(Queue approvalQueue, DirectExchange defaultExchange) {
        return org.springframework.amqp.core.BindingBuilder.bind(approvalQueue).to(defaultExchange).with("approval");
    }

    @Bean
    public Binding userLoginEventBinding(Queue userLoginEventQueue, DirectExchange defaultExchange) {
        return org.springframework.amqp.core.BindingBuilder.bind(userLoginEventQueue).to(defaultExchange).with("event.user.login");
    }

    @Bean
    public Binding orderStatusChangeEventBinding(Queue orderStatusChangeEventQueue, DirectExchange defaultExchange) {
        return org.springframework.amqp.core.BindingBuilder.bind(orderStatusChangeEventQueue).to(defaultExchange).with("event.order.status");
    }
}
