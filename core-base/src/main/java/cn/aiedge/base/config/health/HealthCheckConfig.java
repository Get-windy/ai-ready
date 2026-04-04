package cn.aiedge.base.config.health;

import org.springframework.boot.actuate.endpoint.web.WebEndpointProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 健康检查配置类
 * 
 * 配置内容：
 * 1. Actuator 端点路径配置
 * 2. 健康检查响应格式
 * 3. 健康检查访问权限
 * 
 * @author devops-engineer
 * @since 1.0.0
 */
@Configuration
public class HealthCheckConfig implements WebMvcConfigurer {

    /**
     * 配置健康检查端点路径匹配
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 允许 actuator 端点访问
        configurer.addPathPrefix("/actuator", c -> 
            c.isAnnotationPresent(org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint.class) ||
            c.isAnnotationPresent(org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint.class)
        );
    }
}