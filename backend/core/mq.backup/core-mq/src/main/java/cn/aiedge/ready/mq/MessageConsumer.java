package cn.aiedge.ready.mq;

/**
 * 消息消费者接口
 */
public interface MessageConsumer {

    /**
     * 注册消息处理器
     *
     * @param queueName 队列名称
     * @param handler 消息处理器
     */
    void registerHandler(String queueName, MessageHandler handler);

    /**
     * 处理消息的回调接口
     */
    @FunctionalInterface
    interface MessageHandler {
        void handleMessage(String message);
    }
}