package cn.aiedge.automation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("cn.aiedge.automation.mapper")
public class AutomationApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutomationApplication.class, args);
    }
}