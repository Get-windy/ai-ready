package cn.aiedge.test.connectionpool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 数据库连接池性能测试应用启动类
 */
@SpringBootApplication
public class ConnectionPoolTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConnectionPoolTestApplication.class, args);
    }
}
