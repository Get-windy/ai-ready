package cn.aiedge.ready.mq.example;

import cn.aiedge.ready.mq.MessageConsumer;
import cn.aiedge.ready.mq.MessageProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 消息队列使用示例
 */
@Component
public class MessageExample {

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private MessageConsumer messageConsumer;

    @PostConstruct
    public void init() {
        // 注册消息处理器
        messageConsumer.registerHandler("example_queue", message -> {
            System.out.println("收到消息：" + message);
            // 处理业务逻辑
            processMessage(message);
        });

        // 发送示例消息
        sendMessageExample();
    }

    /**
     * 发送消息示例
     */
    public void sendMessageExample() {
        // 发送普通消息
        messageProducer.sendMessage("example_queue", "Hello from example!");

        // 发送包含对象的消息
        ExampleMessage obj = new ExampleMessage("test-id", "这是一个示例消息", System.currentTimeMillis());
        messageProducer.sendMessage("example_queue", obj);

        // 发送延迟消息（5秒后执行）
        messageProducer.sendDelayedMessage("delay_queue", "延迟消息示例", 5000);

        // 发送到交换机
        messageProducer.sendToExchange("example_exchange", "routing.key", "交换机消息示例");
    }

    /**
     * 处理消息
     */
    private void processMessage(String message) {
        System.out.println("正在处理消息：" + message);
        // 实现具体的业务逻辑
    }

    /**
     * 示例消息对象
     */
    public static class ExampleMessage {
        private String id;
        private String content;
        private long timestamp;

        public ExampleMessage() {}

        public ExampleMessage(String id, String content, long timestamp) {
            this.id = id;
            this.content = content;
            this.timestamp = timestamp;
        }

        // getter和setter方法
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        @Override
        public String toString() {
            return "ExampleMessage{" +
                    "id='" + id + '\'' +
                    ", content='" + content + '\'' +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
}
