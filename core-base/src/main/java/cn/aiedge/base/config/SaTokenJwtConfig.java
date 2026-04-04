package cn.aiedge.base.config;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token JWT 配置
 * 整合 JWT 模式，实现无状态认证
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
public class SaTokenJwtConfig {

    /**
     * Sa-Token 整合 JWT (Simple 简单模式)
     * - Simple 模式：Token 风格为 JWT，但会话数据仍存储在 Redis
     * - 适用场景：需要 Token 自包含信息，同时保留 Redis 会话管理
     */
    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple();
    }
}