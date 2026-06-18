package cn.aiedge.common.log;import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求日志过滤器
 * 为每个请求设置追踪上下文，记录请求耗时
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "ai-ready.log", name = "request-logging", havingValue = "true", matchIfMissing = true)
public class RequestLoggingFilter extends OncePerRequestFilter {

    /** 慢请求阈值（毫秒） */
    private static final long SLOW_REQUEST_THRESHOLD = 1000;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                     FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        
        try {
            // 初始化日志上下文
            initLogContext(request);
            
            log.debug("请求开始: {} {}", request.getMethod(), request.getRequestURI());
            
            filterChain.doFilter(request, response);
            
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            
            // 记录请求日志
            recordRequestLog(request, status, duration);
            
            // 清理上下文
            LogContext.clear();
        }
    }

    /**
     * 初始化日志上下文
     */
    private void initLogContext(HttpServletRequest request) {
        // 追踪ID：优先从请求头获取（链路追踪）
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        LogContext.setTraceId(traceId);
        
        // Span ID
        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        LogContext.setSpanId(spanId);
        
        // 请求ID
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString().replace("-", "");
        }
        LogContext.setRequestId(requestId);
        
        // 客户端IP
        String clientIp = getClientIp(request);
        LogContext.put("clientIp", clientIp);
        
        // 设置响应头
        // Note: response对象在finally中可用
    }

    /**
     * 记录请求日志
     */
    private void recordRequestLog(HttpServletRequest request, int status, long duration) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String fullUrl = query != null ? uri + "?" + query : uri;
        
        if (duration > SLOW_REQUEST_THRESHOLD) {
            log.warn("慢请求: {} {} - status={}, duration={}ms", method, fullUrl, status, duration);
        } else if (status >= 500) {
            log.error("请求异常: {} {} - status={}, duration={}ms", method, fullUrl, status, duration);
        } else if (status >= 400) {
            log.warn("客户端错误: {} {} - status={}, duration={}ms", method, fullUrl, status, duration);
        } else {
            log.debug("请求完成: {} {} - status={}, duration={}ms", method, fullUrl, status, duration);
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多层代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
