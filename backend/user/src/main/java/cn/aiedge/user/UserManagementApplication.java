package cn.aiedge.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 用户管理模块Spring Boot应用主类
 * 
 * @author AI-Ready Team
 * @version 1.0.0
 * @since 2026-04-25
 */
@SpringBootApplication
@EntityScan(basePackages = "cn.aiedge.user.entity")
@EnableJpaRepositories(basePackages = "cn.aiedge.user.repository")
public class UserManagementApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(UserManagementApplication.class, args);
    }
}