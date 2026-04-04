package cn.aiedge.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * API性能优化配置
 * 
 * 包含：
 * - 响应时间监控
 * - 响应压缩配置
 * - 请求日志优化
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class ApiPerformanceConfig {

    /**
     * API响应时间监控过滤器
     * 
     * 记录每个API请求的响应时间，用于性能分析
     */
    @Bean
    public FilterRegistrationBean<ApiTimingFilter> apiTimingFilter() {
        FilterRegistrationBean<ApiTimingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiTimingFilter());
        registration.addUrlPatterns("/api/*");
        registration.setName("apiTimingFilter");
        registration.setOrder(1);
        return registration;
    }

    /**
     * API响应时间监控过滤器实现
     */
    public static class ApiTimingFilter extends OncePerRequestFilter {

        private static final long WARN_THRESHOLD_MS = 200; // 警告阈值
        private static final long SLOW_THRESHOLD_MS = 500; // 慢请求阈值

        @Override
        protected void doFilterInternal(HttpServletRequest request, 
                                        HttpServletResponse response, 
                                        FilterChain filterChain) 
                throws ServletException, IOException {
            
            long startTime = System.nanoTime();
            String requestId = generateRequestId();
            
            // 设置响应头便于追踪
            response.setHeader("X-Request-ID", requestId);
            
            try {
                filterChain.doFilter(request, response);
            } finally {
                long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                String method = request.getMethod();
                String uri = request.getRequestURI();
                int status = response.getStatus();
                
                // 记录响应时间
                logApiTiming(method, uri, status, durationMs);
                
                // 设置响应时间头（便于前端监控）
                response.setHeader("X-Response-Time-Ms", String.valueOf(durationMs));
                
                // 慢请求告警
                if (durationMs > SLOW_THRESHOLD_MS) {
                    log.warn("[SLOW-API] {} {} took {}ms (status={})", method, uri, durationMs, status);
                } else if (durationMs > WARN_THRESHOLD_MS) {
                    log.debug("[WARN-API] {} {} took {}ms (status={})", method, uri, durationMs, status);
                }
            }
        }

        private String generateRequestId() {
            return String.format("%08x", System.nanoTime() & 0xFFFFFFFFL);
        }

        private void logApiTiming(String method, String uri, int status, long durationMs) {
            // INFO级别记录关键API响应时间
            if (uri.startsWith("/api/auth") || uri.startsWith("/api/user") || 
                uri.startsWith("/api/customer") || uri.startsWith("/api/order")) {
                log.info("[API-TIMING] {} {} {}ms (status={})", method, uri, durationMs, status);
            }
        }
    }
}