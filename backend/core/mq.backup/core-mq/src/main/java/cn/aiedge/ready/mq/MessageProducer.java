package cn.aiedge.ready.mq;

/**
 * 消息生产者接口
 */
public interface MessageProducer {

    /**
     * 发送消息到指定队列
     *
     * @param queueName 队列名称
     * @param message 消息内容
     */
    void sendMessage(String queueName, Object message);

    /**
     * 发送延时消息
     *
     * @param queueName 队列名称
     * @param message 消息内容
     * @param delay 延迟时间（毫秒）
     */
    void sendDelayedMessage(String queueName, Object message, long delay);

    /**
     * 发送消息到交换机
     *
     * @param exchange 交换机名称
     * @param routingKey 路由键
     * @param message 消息内容
     */
    void sendToExchange(String exchange, String routingKey, Object message);
}