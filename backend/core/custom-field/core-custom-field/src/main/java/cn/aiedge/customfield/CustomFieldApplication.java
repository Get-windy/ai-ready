package cn.aiedge.customfield;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("cn.aiedge.customfield.mapper")
public class CustomFieldApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomFieldApplication.class, args);
    }
}