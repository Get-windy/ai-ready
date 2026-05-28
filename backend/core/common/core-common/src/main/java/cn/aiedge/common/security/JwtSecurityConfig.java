package cn.aiedge.common.security;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * JWT Token安全配置
 * 
 * 功能：
 * 1. JWT Token配置优化
 * 2. Token过期时间设置
 * 3. Token刷新机制
 * 4. 防止Token重放攻击
 */
@Configuration
public class JwtSecurityConfig {

    /**
     * Sa-Token配置 (仅当启用JWT时)
     */
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
    
    /**
     * 注册JWT的StpLogic (仅当启用JWT时)
     */
    @Bean
    @ConditionalOnProperty(prefix = "sa-token", name = "jwt-enabled", havingValue = "true")
    public StpLogic stpLogicJwt() {
        return new cn.dev33.satoken.jwt.StpLogicJwtForSimple();
    }
    
    /**
     * Token刷新服务
     */
    @Bean
    public TokenRefreshService tokenRefreshService() {
        return new TokenRefreshService();
    }
    
    /**
     * Token黑名单服务
     */
    @Bean
    public TokenBlacklistService tokenBlacklistService() {
        return new TokenBlacklistService();
    }
}