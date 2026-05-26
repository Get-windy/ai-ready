package cn.aiedge.base.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 测试配置类
 * 用于集成测试时提供Spring Boot上下文
 */
@SpringBootApplication
@ComponentScan(basePackages = {"cn.aiedge.base"})
public class TestConfig {
}