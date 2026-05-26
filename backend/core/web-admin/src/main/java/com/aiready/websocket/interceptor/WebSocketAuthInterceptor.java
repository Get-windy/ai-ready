package com.aiready.websocket.interceptor;

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
 * WebSocket认证拦截器
 * 用于在WebSocket握手时进行身份验证
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            
            // 从请求参数或Header中获取token
            String token = servletRequest.getServletRequest().getParameter("token");
            if (token == null || token.isEmpty()) {
                token = servletRequest.getServletRequest().getHeader("Authorization");
                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }
            }
            
            if (token == null || token.isEmpty()) {
                log.warn("WebSocket连接缺少token");
                return false;
            }
            
            try {
                // 验证token并获取用户ID
                Object loginId = StpUtil.getLoginIdByToken(token);
                if (loginId == null) {
                    log.warn("WebSocket连接token无效");
                    return false;
                }
                
                // 将用户ID存入attributes，供后续使用
                attributes.put("userId", Long.valueOf(loginId.toString()));
                attributes.put("token", token);
                
                log.info("WebSocket连接认证成功，用户ID: {}", loginId);
                return true;
            } catch (Exception e) {
                log.error("WebSocket连接认证失败", e);
                return false;
            }
        }
        return false;
    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手后的处理
    }
}
