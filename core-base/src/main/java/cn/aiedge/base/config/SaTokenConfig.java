package cn.aiedge.base.config;

import cn.aiedge.base.security.StpInterfaceImpl;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    private final StpInterfaceImpl stpInterface;

    /**
     * 注册 Sa-Token 拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册安全拦截器（检查Token黑名单）
        registry.addInterceptor(new SecurityInterceptor(stpInterface))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/user/login",
                        "/api/user/register",
                        "/doc.html",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/favicon.ico",
                        "/error"
                )
                .order(0);
        
        // 注册 Sa-Token 拦截器，校验规则为 StpUtil.checkLogin()。
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/user/login",
                        "/api/user/register",
                        "/doc.html",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/favicon.ico",
                        "/error"
                )
                .order(1);
    }
}