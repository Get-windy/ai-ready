package cn.aiedge.erp.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 批次管理模块应用主类
 */
@SpringBootApplication
@EnableJpaAuditing  // 启用JPA审计功能
public class BatchApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }
    
}