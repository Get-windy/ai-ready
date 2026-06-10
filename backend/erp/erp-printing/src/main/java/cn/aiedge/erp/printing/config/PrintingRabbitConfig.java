package cn.aiedge.erp.printing.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 打印模块 RabbitMQ 配置
 *
 * <p>架构说明：
 * <ul>
 *   <li>使用 TopicExchange 实现任务分发的灵活路由</li>
 *   <li>每个客户端一个独立队列 (print.task.{clientId})，确保单任务串行执行</li>
 *   <li>状态回执统一路由到 print.status 队列，集中处理</li>
 *   <li>prefetchCount=1 确保同一队列一次只处理一个消息</li>
 * </ul>
 */
@Configuration
public class PrintingRabbitConfig {

    /** 打印任务交换机 */
    public static final String EXCHANGE_PRINT = "print.topic.exchange";

    /** 任务分发路由键前缀：print.task.{clientId} */
    public static final String ROUTING_KEY_TASK_PREFIX = "print.task.";
    /** 任务分发路由键通配模式：print.task.* */
    public static final String ROUTING_KEY_TASK_PATTERN = "print.task.*";

    /** 状态回执路由键 */
    public static final String ROUTING_KEY_STATUS = "print.status";
    /** 状态回执队列 */
    public static final String QUEUE_STATUS = "print.status.queue";
    /** 状态回执路由键通配 */
    public static final String ROUTING_KEY_STATUS_PATTERN = "print.status.#";

    // ==================== 死信队列 ====================

    /** 死信交换机 */
    public static final String DLX_EXCHANGE = "print.dlx.exchange";
    /** 死信队列 */
    public static final String DLX_QUEUE = "print.dlx.queue";
    /** 死信路由键 */
    public static final String ROUTING_KEY_DLX = "print.dlx.#";

    // ==================== 交换机 ====================

    @Bean
    public TopicExchange printTopicExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_PRINT)
                .durable(true)
                .build();
    }

    // ==================== 状态回执（固定队列） ====================

    @Bean
    public Queue printStatusQueue() {
        return QueueBuilder.durable(QUEUE_STATUS)
                .build();
    }

    @Bean
    public Binding printStatusBinding(
            @Qualifier("printTopicExchange") TopicExchange exchange,
            @Qualifier("printStatusQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(ROUTING_KEY_STATUS_PATTERN);
    }

    // ==================== 死信队列 ====================

    @Bean
    public TopicExchange printDlxExchange() {
        return ExchangeBuilder.topicExchange(DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue printDlxQueue() {
        return QueueBuilder.durable(DLX_QUEUE).build();
    }

    @Bean
    public Binding printDlxBinding(
            @Qualifier("printDlxExchange") TopicExchange exchange,
            @Qualifier("printDlxQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(ROUTING_KEY_DLX);
    }

    // ==================== 任务分发 Bean 声明（客户端队列由分发时动态创建/声明） ====================

    /**
     * 动态声明客户端队列
     * @param amqpAdmin AMQP 管理组件
     * @param clientId 客户端 ID
     */
    public static void declareClientQueue(AmqpAdmin amqpAdmin, Long clientId) {
        String queueName = ROUTING_KEY_TASK_PREFIX + clientId;
        String routingKey = ROUTING_KEY_TASK_PREFIX + clientId;

        Queue queue = QueueBuilder.durable(queueName)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(ROUTING_KEY_DLX)
                .build();
        amqpAdmin.declareQueue(queue);

        Binding binding = BindingBuilder.bind(queue)
                .to(new TopicExchange(EXCHANGE_PRINT))
                .with(routingKey);
        amqpAdmin.declareBinding(binding);
    }

    // ==================== 消息转换器 ====================

    @Bean
    @ConditionalOnMissingBean(RabbitTemplate.class)
    public RabbitTemplate printingRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        // 消息发送确认回调
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.warn("MQ消息发送失败: correlationId={}, cause={}",
                        correlationData != null ? correlationData.getId() : null, cause);
            }
        });
        return template;
    }

    @Bean
    @ConditionalOnMissingBean(SimpleRabbitListenerContainerFactory.class)
    public SimpleRabbitListenerContainerFactory printingListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        // 单任务串行处理
        factory.setPrefetchCount(1);
        // 手动确认模式（等业务处理完再 ack）
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PrintingRabbitConfig.class);
}
