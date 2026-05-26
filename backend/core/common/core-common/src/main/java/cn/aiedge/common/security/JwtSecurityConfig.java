package cn.aiedge.common.security;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
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
     * Sa-Token配置
     */
    @Bean
    @Primary
    public SaTokenConfig saTokenConfig() {
        SaTokenConfig config = new SaTokenConfig();
        
        // 基础配置
        config.setTokenName("Authorization");
        config.setTokenPrefix("Bearer ");
        
        // Token有效期配置（单位：秒）
        config.setTimeout(30 * 60);           // access_token有效期：30分钟
        config.setActiveTimeout(-1);          // 永久活跃（只要操作就刷新）
        config.setActivityTimeout(-1);        // 无操作保持活跃时间：-1表示永久
        
        // Token刷新配置
        config.setIsShare(false);             // 不共享Token
        
        // 安全配置
        config.setTokenStyle("uuid");         // Token风格
        config.setIsPrint(false);             // 不打印配置信息
        
        // JWT相关配置
        config.setJwtSecretKey("ai-ready-secure-jwt-key-2026-change-in-production");
        
        return config;
    }
    
    /**
     * 注册JWT的StpLogic
     */
    @Bean
    public StpLogic stpLogic() {
        return new StpLogicJwtForSimple();
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