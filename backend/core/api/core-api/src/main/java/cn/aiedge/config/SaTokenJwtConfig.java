package cn.aiedge.config;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token JWT认证配置
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
public class SaTokenJwtConfig {

    /**
     * Sa-Token 整合 JWT (Simple 简单模式)
     * 该模式下 Token 风格为：jwt载荷
     */
    @Bean
    @ConditionalOnProperty(prefix = "sa-token", name = "jwt-enabled", havingValue = "true")
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple();
    }
}
