package cn.aiedge.erp.budget;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"cn.aiedge.erp.budget", "cn.aiedge.core"})
@EntityScan(basePackages = {"cn.aiedge.erp.budget.model", "cn.aiedge.core.model"})
@EnableJpaRepositories(basePackages = {"cn.aiedge.erp.budget.repository", "cn.aiedge.core.repository"})
public class BudgetApplication {
    public static void main(String[] args) {
        SpringApplication.run(BudgetApplication.class, args);
    }
}
