package cn.aiedge.erp.invoice.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 发票服务配置类
 */
@Configuration
@ComponentScan(basePackages = "cn.aiedge.erp.invoice.service.impl")
public class InvoiceServiceConfig {
}