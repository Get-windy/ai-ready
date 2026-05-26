package cn.aiedge.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/**
 * API网关路由配置
 * 配置服务路由规则、跨域策略等
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
public class GatewayConfig {

    /**
     * 配置路由规则
     * 
     * @param builder 路由构建器
     * @return 路由定位器
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // 用户服务路由
            .route("user-service", r -> r.path("/api/user/**")
                .uri("lb://user-service"))
            // 认证服务路由
            .route("auth-service", r -> r.path("/api/auth/**")
                .uri("lb://auth-service"))
            // 客户服务路由
            .route("customer-service", r -> r.path("/api/customer/**")
                .uri("lb://customer-service"))
            // CRM服务路由
            .route("crm-service", r -> r.path("/api/crm/**")
                .uri("lb://crm-service"))
            // ERP服务路由
            .route("erp-service", r -> r.path("/api/erp/**")
                .uri("lb://erp-service"))
            // 知识管理服务路由
            .route("knowledge-service", r -> r.path("/api/knowledge/**")
                .uri("lb://knowledge-service"))
            // 文件存储服务路由
            .route("storage-service", r -> r.path("/api/storage/**", "/upload/**", "/download/**")
                .uri("lb://storage-service"))
            // 消息通知服务路由
            .route("notification-service", r -> r.path("/api/notification/**", "/api/message/**")
                .uri("lb://notification-service"))
            // 系统管理服务路由
            .route("system-service", r -> r.path("/api/system/**", "/api/admin/**")
                .uri("lb://system-service"))
            // 报表服务路由
            .route("report-service", r -> r.path("/api/report/**", "/api/dashboard/**")
                .uri("lb://report-service"))
            .build();
    }
}
