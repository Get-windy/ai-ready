package cn.aiedge.erp.price.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ERP价格策略引擎应用主类
 */
@Configuration
@EnableCaching
@EnableScheduling
public class ErpPriceEngineApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ErpPriceEngineApplication.class, args);
    }
}