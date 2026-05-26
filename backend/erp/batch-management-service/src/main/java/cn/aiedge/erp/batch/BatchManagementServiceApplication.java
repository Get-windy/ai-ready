package cn.aiedge.erp.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 批次管理服务启动类
 * 
 * 功能特性：
 * 1. 批次生命周期管理（创建、查询、更新、删除）
 * 2. 批次状态管理（激活、停用、锁定、完成）
 * 3. 批次流转管理（入库、出库、质检、转移）
 * 4. 批次关联管理（生产订单、采购订单、库存）
 * 5. 批次追溯管理（溯源、追踪）
 * 6. 批次预警管理（过期预警、库存预警）
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "cn.aiedge.erp.batch.feign")
@ComponentScan(basePackages = {
    "cn.aiedge.erp.batch",
    "cn.aiedge.erp.common",
    "cn.aiedge.erp.config"
})
@EnableJpaAuditing
public class BatchManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchManagementServiceApplication.class, args);
    }
}