package cn.aiedge.erp.invoice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 发票管理模块主应用类
 * 
 * 功能概述:
 * 1. 发票申请管理
 * 2. 发票审批工作流
 * 3. 发票生成和打印
 * 4. 税务计算和合规性检查
 * 5. 付款跟踪和管理
 * 6. 发票存档和检索
 * 
 * 技术特性:
 * - 基于Spring Boot 3.x
 * - JPA/Hibernate数据持久化
 * - Redis缓存支持
 * - RabbitMQ消息队列
 * - OpenFeign服务调用
 * - 多级审批工作流
 * - PDF/Excel文档生成
 * - QR码生成和验证
 * - 数字签名支持
 */
@SpringBootApplication(
    scanBasePackages = {
        "cn.aiedge.erp.invoice",
        "cn.aiedge.erp.core"
    }
)
@ComponentScan(basePackages = {
    "cn.aiedge.erp.invoice.controller",
    "cn.aiedge.erp.invoice.service.impl",
    "cn.aiedge.erp.invoice.config"
})
@EntityScan(basePackages = {
    "cn.aiedge.erp.invoice.model.entity",
    "cn.aiedge.erp.core.model.entity"
})
@EnableJpaRepositories(basePackages = {
    "cn.aiedge.erp.invoice.repository",
    "cn.aiedge.erp.core.repository"
})
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableFeignClients(basePackages = {
    "cn.aiedge.erp.invoice.integration"
})
public class InvoiceApplication {

    /**
     * 主应用入口
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(InvoiceApplication.class, args);
    }
    
    /**
     * 应用信息
     */
    public static class Info {
        public static final String MODULE_NAME = "ERP Invoice Management Module";
        public static final String VERSION = "1.0.0-SNAPSHOT";
        public static final String DESCRIPTION = "Comprehensive invoice management system for ERP";
        public static final String DEVELOPER = "AI-Ready Team";
        public static final String CONTACT = "support@ai-ready.com";
        
        public static void printBanner() {
            System.out.println("=========================================");
            System.out.println("    " + MODULE_NAME);
            System.out.println("    Version: " + VERSION);
            System.out.println("    " + DESCRIPTION);
            System.out.println("=========================================");
            System.out.println("Developer: " + DEVELOPER);
            System.out.println("Contact: " + CONTACT);
            System.out.println("=========================================");
        }
    }
}