package com.aiready.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * 安全配置类
 */
@Configuration
public class SecurityConfig {

    @Value("${security.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${security.cors.allowed-methods:*}")
    private String allowedMethods;

    @Value("${security.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${security.cors.allow-credentials:true}")
    private Boolean allowCredentials;

    @Value("${security.cors.max-age:3600}")
    private Long maxAge;

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS配置
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 允许的域名
        if ("*".equals(allowedOrigins)) {
            config.addAllowedOriginPattern("*");
        } else {
            Arrays.stream(allowedOrigins.split(","))
                  .forEach(config::addAllowedOrigin);
        }

        // 允许的HTTP方法
        if ("*".equals(allowedMethods)) {
            config.addAllowedMethod("*");
        } else {
            Arrays.stream(allowedMethods.split(","))
                  .forEach(config::addAllowedMethod);
        }

        // 允许的请求头
        if ("*".equals(allowedHeaders)) {
            config.addAllowedHeader("*");
        } else {
            Arrays.stream(allowedHeaders.split(","))
                  .forEach(config::addAllowedHeader);
        }

        // 允许携带凭证
        config.setAllowCredentials(allowCredentials);

        // 预检请求缓存时间
        config.setMaxAge(maxAge);

        // 暴露的响应头
        config.addExposedHeader("Authorization");
        config.addExposedHeader("X-Timestamp");
        config.addExposedHeader("X-Nonce");
        config.addExposedHeader("X-Signature");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}