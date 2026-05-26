package cn.aiedge.erp.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 财务管理模块主应用类
 * 
 * 功能概述:
 * 1. 统一财务接口网关
 * 2. 账户管理
 * 3. 交易管理
 * 4. 财务报表
 * 5. 税务管理
 * 6. 财务分析
 * 
 * 技术特性:
 * - 基于Spring Boot 3.x
 * - JPA/Hibernate数据持久化
 * - Redis缓存支持
 * - RabbitMQ消息队列
 * - OpenFeign服务调用
 * - 多模块整合
 * - 统一API网关
 */
@SpringBootApplication(
    scanBasePackages = {
        "cn.aiedge.erp.finance",
        "cn.aiedge.erp.core"
    }
)
@EntityScan(basePackages = {
    "cn.aiedge.erp.finance.model.entity",
    "cn.aiedge.erp.core.model.entity"
})
@EnableJpaRepositories(basePackages = {
    "cn.aiedge.erp.finance.repository",
    "cn.aiedge.erp.core.repository"
})
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableFeignClients(basePackages = {
    "cn.aiedge.erp.finance.integration"
})
public class FinanceApplication {

    /**
     * 主应用入口
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(FinanceApplication.class, args);
    }
    
    /**
     * 应用信息
     */
    public static class Info {
        public static final String MODULE_NAME = "ERP Finance Unified Module";
        public static final String VERSION = "1.0.0-SNAPSHOT";
        public static final String DESCRIPTION = "Comprehensive finance management system for ERP";
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
