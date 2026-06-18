// JWT模式暂时禁用（开发环境使用默认Token模式）
// 生产环境启用时需取消注释并添加 sa-token-jwt 依赖
/*
package cn.aiedge.common.security;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JwtSecurityConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "sa-token", name = "jwt-enabled", havingValue = "true")
    public SaTokenConfig saTokenConfigJwt() {
        SaTokenConfig config = new SaTokenConfig();
        config.setTokenName("Authorization");
        config.setTimeout(86400);
        config.setActiveTimeout(-1);
        config.setIsShare(true);
        config.setTokenStyle("uuid");
        config.setIsLog(false);
        config.setJwtSecretKey("ai-ready-secure-jwt-key-2026-change-in-production");
        return config;
    }

    @Bean
    @ConditionalOnProperty(prefix = "sa-token", name = "jwt-enabled", havingValue = "true")
    public StpLogic stpLogicJwt() {
        return new cn.dev33.satoken.jwt.StpLogicJwtForSimple();
    }

    @Bean
    public TokenRefreshService tokenRefreshService() {
        return new TokenRefreshService();
    }

    @Bean
    public TokenBlacklistService tokenBlacklistService() {
        return new TokenBlacklistService();
    }
}
*/