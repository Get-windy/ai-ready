package cn.aiedge.erp.purchase.return;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 采购换货管理模块 - Spring Boot 应用启动类
 * 
 * 功能: 提供采购订单收货后的质量换货、数量补货、规格更换等功能
 * 集成: 与采购管理、库存管理、供应商管理、质量管理等模块集成
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"cn.aiedge.erp"})
@ComponentScan(basePackages = {"cn.aiedge.erp.purchase.return", "cn.aiedge.common"})
public class PurchaseReturnApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PurchaseReturnApplication.class, args);
    }
}