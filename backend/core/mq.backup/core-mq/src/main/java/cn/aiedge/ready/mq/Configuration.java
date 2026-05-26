package cn.aiedge.ready.mq;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 消息队列配置类
 */
@Component
@ConfigurationProperties(prefix = "aiedge.mq")
public class Configuration {

    /**
     * 消息队列类型 (rabbitmq, redis, kafka)
     */
    private String type = "rabbitmq";

    /**
     * RabbitMQ配置
     */
    private Rabbitmq rabbitmq = new Rabbitmq();

    /**
     * Redis配置
     */
    private Redis redis = new Redis();

    /**
     * Kafka配置
     */
    private Kafka kafka = new Kafka();

    // getter和setter方法
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Rabbitmq getRabbitmq() {
        return rabbitmq;
    }

    public void setRabbitmq(Rabbitmq rabbitmq) {
        this.rabbitmq = rabbitmq;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public void setKafka(Kafka kafka) {
        this.kafka = kafka;
    }

    public static class Rabbitmq {
        private String host = "localhost";
        private int port = 5672;
        private String username = "guest";
        private String password = "guest";
        private String virtualHost = "/";
        private boolean publisherConfirms = true;
        private boolean publisherReturns = true;

        // getter和setter方法
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getVirtualHost() { return virtualHost; }
        public void setVirtualHost(String virtualHost) { this.virtualHost = virtualHost; }
        public boolean isPublisherConfirms() { return publisherConfirms; }
        public void setPublisherConfirms(boolean publisherConfirms) { this.publisherConfirms = publisherConfirms; }
        public boolean isPublisherReturns() { return publisherReturns; }
        public void setPublisherReturns(boolean publisherReturns) { this.publisherReturns = publisherReturns; }
    }

    public static class Redis {
        private String host = "localhost";
        private int port = 6379;
        private String password = "";
        private int database = 0;

        // getter和setter方法
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public int getDatabase() { return database; }
        public void setDatabase(int database) { this.database = database; }
    }

    public static class Kafka {
        private String bootstrapServers = "localhost:9092";

        // getter和setter方法
        public String getBootstrapServers() { return bootstrapServers; }
        public void setBootstrapServers(String bootstrapServers) { this.bootstrapServers = bootstrapServers; }
    }
}