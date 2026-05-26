package cn.aiedge.erp.printing;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("cn.aiedge.erp.printing.mapper")
public class PrintingApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrintingApplication.class, args);
    }
}