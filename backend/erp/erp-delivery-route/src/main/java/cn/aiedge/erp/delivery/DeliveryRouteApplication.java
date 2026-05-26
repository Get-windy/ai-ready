package cn.aiedge.erp.delivery;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("cn.aiedge.erp.delivery.mapper")
public class DeliveryRouteApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliveryRouteApplication.class, args);
    }
}