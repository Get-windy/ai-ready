package cn.aiedge.erp.expense;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"cn.aiedge.erp.expense", "cn.aiedge.core"})
@EntityScan(basePackages = {"cn.aiedge.erp.expense.model", "cn.aiedge.core.model"})
@EnableJpaRepositories(basePackages = {"cn.aiedge.erp.expense.repository", "cn.aiedge.core.repository"})
public class ExpenseApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExpenseApplication.class, args);
    }
}