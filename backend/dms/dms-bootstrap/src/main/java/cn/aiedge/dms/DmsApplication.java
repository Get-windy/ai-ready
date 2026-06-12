package cn.aiedge.dms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 配送管理系统 (DMS) 启动入口
 *
 * 独立服务，与 ERP、WMS 平行部署。
 * 管理运力池、订单分配、配送执行、签收收款、费用结算的全链路。
 *
 * @author AI-Ready Team
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = {"cn.aiedge.dms", "cn.aiedge.base", "cn.aiedge.common"})
public class DmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DmsApplication.class, args);
    }
}
