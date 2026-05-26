package cn.aiedge.ready.mq.health;

import cn.aiedge.ready.mq.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 消息队列健康检查指示器
 */
@Component
public class MqHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(MqHealthIndicator.class);

    @Autowired
    private MessageProducer messageProducer;

    @Override
    public Health health() {
        try {
            // 尝试发送一个测试消息来验证连接
            testConnection();
            return Health.up()
                    .withDetail("status", "消息队列服务正常")
                    .withDetail("component", "RabbitMQ")
                    .build();
        } catch (Exception e) {
            logger.error("消息队列健康检查失败", e);
            return Health.down()
                    .withDetail("status", "消息队列服务异常")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    /**
     * 测试连接
     */
    private void testConnection() {
        try {
            // 发送一个测试消息到测试队列
            messageProducer.sendMessage("health_check_queue", 
                "Health check message at " + System.currentTimeMillis());
            logger.debug("消息队列连接测试成功");
        } catch (Exception e) {
            logger.error("消息队列连接测试失败", e);
            throw e;
        }
    }
}