package cn.aiedge.erp.sale.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 价格策略模块自动配置
 * 启用价格策略管理功能的Spring Bean扫描
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = {
        "cn.aiedge.erp.sale.controller",
        "cn.aiedge.erp.sale.service",
        "cn.aiedge.erp.sale.mapper"
})
public class PricingConfig {
    // 自动配置通过 @ComponentScan 完成
}
