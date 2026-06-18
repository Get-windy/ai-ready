package cn.aiedge.architecture.checker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 架构合规性检查工具主应用程序
 * 
 * <p>启动类，负责初始化Spring Boot应用程序</p>
 * 
 * @author Architecture Team
 * @version 1.0.0
 * @since 2026-05-05
 */
@SpringBootApplication
@EnableScheduling
public class Application {

    /**
     * 应用程序主入口
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}