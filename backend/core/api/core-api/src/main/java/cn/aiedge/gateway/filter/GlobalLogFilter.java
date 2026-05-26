package cn.aiedge.gateway.filter;

import cn.aiedge.gateway.service.GatewayLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 全局日志过滤器
 * 记录所有经过网关的请求信息，用于监控和审计
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Component
public class GlobalLogFilter implements GlobalFilter, Ordered {

    @Autowired
    private GatewayLogService gatewayLogService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 生成请求ID
        String requestId = UUID.randomUUID().toString();
        
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        
        // 获取请求信息
        String method = exchange.getRequest().getMethod() != null ? exchange.getRequest().getMethod().name() : "UNKNOWN";
        String path = exchange.getRequest().getURI().getPath();
        String queryString = exchange.getRequest().getURI().getQuery();
        String remoteAddr = getRemoteAddress(exchange);
        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
        String referer = exchange.getRequest().getHeaders().getFirst("Referer");

        // 继续执行请求链
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // 请求完成后记录日志
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = exchange.getResponse().getStatusCode() != null ? 
                exchange.getResponse().getStatusCode().value() : 0;
            
            // 记录访问日志
            gatewayLogService.logAccess(
                requestId,
                method,
                path,
                queryString,
                remoteAddr,
                userAgent,
                referer,
                statusCode,
                duration,
                LocalDateTime.now()
            );
        }));
    }

    @Override
    public int getOrder() {
        return -1; // 在其他过滤器之前执行
    }

    /**
     * 获取客户端真实IP地址
     * 
     * @param exchange 服务器Web交换
     * @return 客户端IP地址
     */
    private String getRemoteAddress(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }
}
