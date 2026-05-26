package cn.aiedge.erp.party;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 往来单位管理模块启动类
 * 
 * @author AI-Ready Team
 * @date 2026-05-25
 */
@SpringBootApplication
@MapperScan("cn.aiedge.erp.party.mapper")
public class PartyApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PartyApplication.class, args);
    }
}
