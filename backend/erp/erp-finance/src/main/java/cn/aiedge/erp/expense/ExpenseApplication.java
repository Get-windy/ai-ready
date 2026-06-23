package cn.aiedge.erp.expense;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

@Configuration
@ComponentScan(basePackages = {"cn.aiedge.erp.expense", "cn.aiedge.core"})
@EntityScan(basePackages = {"cn.aiedge.erp.expense.model", "cn.aiedge.core.model"})
public class ExpenseApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExpenseApplication.class, args);
    }
}