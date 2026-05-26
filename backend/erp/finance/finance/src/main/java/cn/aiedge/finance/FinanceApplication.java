package cn.aiedge.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 财务管理模块启动类
 */
@SpringBootApplication(scanBasePackages = {"cn.aiedge.finance", "cn.aiedge.core"})
@EnableCaching
@EnableScheduling
@EnableFeignClients(basePackages = {"cn.aiedge.finance", "cn.aiedge.core"})
public class FinanceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(FinanceApplication.class, args);
    }
    
}