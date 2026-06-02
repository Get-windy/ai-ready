package cn.aiedge.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Bearer Token 标准化过滤器
 * 将 Authorization 头中的 "Bearer xxx" 自动转换为 "xxx"，确保 Sa-Token 正确识别
 */
@Component
@Order(-100)
public class BearerTokenFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String authHeader = httpRequest.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // 去掉 "Bearer " 前缀
            request = new HttpServletRequestWrapper(httpRequest) {
                @Override
                public String getHeader(String name) {
                    if ("Authorization".equalsIgnoreCase(name)) {
                        return token;
                    }
                    return super.getHeader(name);
                }
            };
        }

        chain.doFilter(request, response);
    }
}
