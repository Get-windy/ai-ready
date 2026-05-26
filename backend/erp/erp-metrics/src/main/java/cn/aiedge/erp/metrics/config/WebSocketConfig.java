package cn.aiedge.erp.metrics.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import orgimport org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 * 支持实时指标数据推送
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MetricsWebSocketHandler metricsWebSocketHandler;

    public WebSocketConfig(MetricsWebSocketHandler metricsWebSocketHandler) {
        this.metricsWebSocketHandler = metricsWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(metricsWebSocketHandler, "/ws/metrics")
                .setAllowedOrigins("*");
    }
}
}
