// JWT模式暂时禁用（开发环境使用默认Token模式）
// 生产环境启用时需取消注释并添加 sa-token-jwt 依赖
/*
package cn.aiedge.config;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SaTokenJwtConfig {

    @Bean
    @ConditionalOnProperty(prefix = "sa-token", name = "jwt-enabled", havingValue = "true")
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple();
    }
}
*/
