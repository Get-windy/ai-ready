package cn.aiedge.erp.supplierbatchanalysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 供应商绩效与批次质量关联分析系统启动类
 *
 * @author team-member
 * @date 2026-04-30
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "cn.aiedge.erp.supplierbatchanalysis",
    "cn.aiedge.common",
    "cn.aiedge.core"
})
@EnableFeignClients(basePackages = "cn.aiedge.erp.supplierbatchanalysis.feign")
public class SupplierBatchAnalysisApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupplierBatchAnalysisApplication.class, args);
    }
}