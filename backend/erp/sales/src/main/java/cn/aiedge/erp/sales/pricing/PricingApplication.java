package cn.aiedge.erp.sales.pricing;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * 价格策略管理模块启动类
 */
@Configuration
@ComponentScan(basePackages = {"cn.aiedge.erp.sales.pricing"})
@EntityScan(basePackages = {"cn.aiedge.erp.sales.pricing.entity"})
public class PricingApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PricingApplication.class, args);
    }
}