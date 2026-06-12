package cn.aiedge.erp.b2b;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"cn.aiedge.erp.b2b", "cn.aiedge.core", "cn.aiedge.base"})
public class B2BMallApplication {
    public static void main(String[] args) {
        SpringApplication.run(B2BMallApplication.class, args);
    }
}
