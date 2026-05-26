package cn.aiedge.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * API网关安全配置
 * 配置认证、授权、限流等安全策略
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    /**
     * 配置安全过滤器链
     * 
     * @param http HTTP安全配置
     * @return 安全过滤器链
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .authorizeExchange(exchanges -> exchanges
                // 允许访问健康检查端点
                .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                // 允许访问Swagger文档
                .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/webjars/**").permitAll()
                // 允许访问认证相关接口
                .pathMatchers("/api/auth/**", "/api/oauth/**").permitAll()
                // 其他请求需要认证
                .anyExchange().authenticated()
            )
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable()); // CORS将在单独的配置中处理

        return http.build();
    }
}
