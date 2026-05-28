package cn.aiedge.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Web安全配置
 * 配置认证、授权等安全策略
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // 允许访问健康检查端点
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // 允许访问Swagger文档
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**").permitAll()
                // 允许访问认证相关接口
                .requestMatchers("/auth/**", "/api/auth/**", "/oauth/**", "/api/oauth/**").permitAll()
                // 允许登录页面
                .requestMatchers("/login", "/login/**").permitAll()
                // 允许静态资源
                .requestMatchers("/static/**", "/public/**", "/favicon.ico").permitAll()
                // 允许错误页面
                .requestMatchers("/error").permitAll()
                // 允许XXL-Job接口
                .requestMatchers("/xxl-job-admin/**").permitAll()
                // 其他请求需要认证
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable());

        return http.build();
    }
}