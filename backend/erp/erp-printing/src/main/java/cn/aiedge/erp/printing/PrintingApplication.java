package cn.aiedge.erp.printing;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"cn.aiedge.erp.printing", "cn.aiedge.base"})
public class PrintingApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrintingApplication.class, args);
    }
}