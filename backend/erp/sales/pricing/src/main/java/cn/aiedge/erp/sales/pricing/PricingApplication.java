package cn.aiedge.erp.sales.pricing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 价格策略管理模块启动类
 */
@SpringBootApplication
@ComponentScan(basePackages = {"cn.aiedge.erp.sales.pricing"})
@EntityScan(basePackages = {"cn.aiedge.erp.sales.pricing.entity"})
@EnableJpaRepositories(basePackages = {"cn.aiedge.erp.sales.pricing.repository"})
public class PricingApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PricingApplication.class, args);
    }
}