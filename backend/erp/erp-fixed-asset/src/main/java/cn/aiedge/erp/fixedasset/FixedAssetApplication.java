package cn.aiedge.erp.fixedasset;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"cn.aiedge.erp.fixedasset", "cn.aiedge.core"})
@EntityScan(basePackages = {"cn.aiedge.erp.fixedasset.model", "cn.aiedge.core.model"})
@EnableJpaRepositories(basePackages = {"cn.aiedge.erp.fixedasset.repository", "cn.aiedge.core.repository"})
public class FixedAssetApplication {
    public static void main(String[] args) {
        SpringApplication.run(FixedAssetApplication.class, args);
    }
}
