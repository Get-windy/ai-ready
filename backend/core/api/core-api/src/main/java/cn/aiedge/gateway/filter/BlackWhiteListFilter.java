package cn.aiedge.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.HashSet;

/**
 * 黑白名单过滤器
 * 实现IP黑白名单功能，控制访问权限
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Component
public class BlackWhiteListFilter implements GlobalFilter, Ordered {

    // 黑名单IP集合
    private final Set<String> blackList = new HashSet<>();

    // 白名单IP集合
    private final Set<String> whiteList = new HashSet<>();

    // 初始化黑白名单
    {
        // 示例黑名单IP
        blackList.add("192.168.1.100");
        blackList.add("10.0.0.50");

        // 示例白名单IP
        whiteList.add("192.168.1.1");
        whiteList.add("127.0.0.1");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = getClientIp(exchange);

        // 检查黑名单
        if (blackList.contains(clientIp)) {
            // 如果在黑名单中，直接拒绝访问
            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        // 检查白名单（如果启用了白名单模式）
        if (!whiteList.isEmpty() && !whiteList.contains(clientIp)) {
            // 如果不在白名单中，拒绝访问
            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 通过检查，继续执行后续过滤器
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -2; // 在日志过滤器之前执行
    }

    /**
     * 获取客户端真实IP地址
     * 
     * @param exchange 服务器Web交换
     * @return 客户端IP地址
     */
    private String getClientIp(ServerWebExchange exchange) {
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
