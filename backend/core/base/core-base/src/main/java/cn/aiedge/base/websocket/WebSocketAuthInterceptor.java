package cn.aiedge.base.websocket;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手认证拦截器
 *
 * 从 URL query param 或 Authorization header 中提取 token，
 * 通过 Sa-Token 的 StpUtil.getLoginIdByToken() 验证登录状态。
 * 认证通过后将 userId 存入 session attributes 供后续处理器使用。
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest)) {
            return false;
        }

        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;

        // 1. 提取 token：优先 URL query param，其次 Authorization header
        String token = servletRequest.getServletRequest().getParameter("token");
        if (token == null || token.isEmpty()) {
            token = servletRequest.getServletRequest().getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
        }

        if (token == null || token.isEmpty()) {
            log.warn("WebSocket 握手失败：缺少 token");
            return false;
        }

        // 2. 通过 Sa-Token 验证
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                log.warn("WebSocket 握手失败：token 无效");
                return false;
            }

            // 3. 存入 session 属性供后续使用
            Long userId = Long.valueOf(loginId.toString());
            attributes.put("userId", userId);
            attributes.put("token", token);

            log.info("WebSocket 连接认证成功，用户ID: {}", userId);
            return true;
        } catch (Exception e) {
            log.error("WebSocket 认证异常", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需额外处理
    }
}
