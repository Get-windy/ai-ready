package cn.aiedge.erp.fixedasset;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

@Configuration
@ComponentScan(basePackages = {"cn.aiedge.erp.fixedasset", "cn.aiedge.core"})
@EntityScan(basePackages = {"cn.aiedge.erp.fixedasset.model", "cn.aiedge.core.model"})
public class FixedAssetApplication {
    public static void main(String[] args) {
        SpringApplication.run(FixedAssetApplication.class, args);
    }
}
