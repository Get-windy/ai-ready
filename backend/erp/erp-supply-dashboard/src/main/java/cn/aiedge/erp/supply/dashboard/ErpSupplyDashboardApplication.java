package cn.aiedge.erp.supply.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ERP供应链数据可视化仪表板应用启动类
 * 
 * @author AI-Edge
 * @since 2026-05-04
 */
@SpringBootApplication
@EntityScan(basePackages = {
    "cn.aiedge.erp.supply.dashboard.entity",
    "cn.aiedge.erp.purchase.entity",
    "cn.aiedge.erp.inventory.entity",
    "cn.aiedge.erp.logistics.entity",
    "cn.aiedge.erp.finance.entity"
})
@EnableJpaRepositories(basePackages = {
    "cn.aiedge.erp.supply.dashboard.repository",
    "cn.aiedge.erp.purchase.repository",
    "cn.aiedge.erp.inventory.repository",
    "cn.aiedge.erp.logistics.repository",
    "cn.aiedge.erp.finance.repository"
})
@ComponentScan(basePackages = {
    "cn.aiedge.erp.supply.dashboard",
    "cn.aiedge.erp.core",
    "cn.aiedge.erp.purchase",
    "cn.aiedge.erp.inventory",
    "cn.aiedge.erp.logistics",
    "cn.aiedge.erp.finance"
})
@EnableFeignClients(basePackages = {
    "cn.aiedge.erp.supply.dashboard.feign",
    "cn.aiedge.erp.core.feign"
})
@EnableScheduling
public class ErpSupplyDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpSupplyDashboardApplication.class, args);
    }

}