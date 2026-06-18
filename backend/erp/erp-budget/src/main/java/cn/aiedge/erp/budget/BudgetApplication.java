package cn.aiedge.erp.budget;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

@Configuration
@ComponentScan(basePackages = {"cn.aiedge.erp.budget", "cn.aiedge.core"})
@EntityScan(basePackages = {"cn.aiedge.erp.budget.model", "cn.aiedge.core.model"})
public class BudgetApplication {
    public static void main(String[] args) {
        SpringApplication.run(BudgetApplication.class, args);
    }
}
