package cn.aiedge.ready.mq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 消息队列模块启动类
 */
@SpringBootApplication
public class MqApplication {

    public static void main(String[] args) {
        SpringApplication.run(MqApplication.class, args);
        System.out.println("企智连消息队列模块启动成功！");
    }
}