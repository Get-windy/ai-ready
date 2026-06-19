package cn.aiedge.permission.filter;

import cn.aiedge.permission.service.PermissionSimulationService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 权限模拟过滤器
 * <p>
 * 自动解析请求头中的 {@code X-Simulate-User-Id}，若存在则自动启用权限模拟。
 * 请求结束后自动清理 ThreadLocal，防止内存泄漏。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@Order(Integer.MIN_VALUE + 50) // 在认证过滤器之后，业务逻辑之前
@RequiredArgsConstructor
public class PermissionSimulationFilter implements Filter {

    private final PermissionSimulationService simulationService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpRequest) {
            try {
                String simulateUserId = httpRequest.getHeader(PermissionSimulationService.SIMULATE_USER_HEADER);
                String simulateReason = httpRequest.getHeader(PermissionSimulationService.SIMULATE_REASON_HEADER);

                if (simulateUserId != null && !simulateUserId.isEmpty()) {
                    simulationService.applyFromRequest(simulateUserId, simulateReason);
                }

                chain.doFilter(request, response);
            } finally {
                // 确保请求结束或异常时清理 ThreadLocal
                simulationService.cleanup();
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
