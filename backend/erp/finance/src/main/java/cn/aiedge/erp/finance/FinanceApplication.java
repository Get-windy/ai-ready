package cn.aiedge.erp.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 财务管理模块主应用类 (已合并到主应用，此入口保留用于本地独立调试)
 */
@SpringBootApplication(
    scanBasePackages = {
        "cn.aiedge.erp.finance",
        "cn.aiedge.base"
    }
)
@EnableCaching
@EnableAsync
@EnableScheduling
public class FinanceApplication {

    /**
     * 主应用入口
     */
    public static void main(String[] args) {
        SpringApplication.run(FinanceApplication.class, args);
    }
}
