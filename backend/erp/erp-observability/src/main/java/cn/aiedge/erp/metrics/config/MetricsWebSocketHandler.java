package cn.aiedge.erp.metrics.config;

import cn.aiedge.erp.metrics.dto.MetricValueDTO;
import cn.aiedge.erp.metrics.service.MetricsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 指标数据WebSocket处理器
 * 支持实时推送指标数据到客户端
 */
@Component
@Slf4j
public class MetricsWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final Map<String, Set<WebSocketSession>> metricSubscriptions = new ConcurrentHashMap<>();
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public MetricsWebSocketHandler(MetricsService metricsService) {
        this.metricsService = metricsService;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 启动定时推送任务
        startMetricsPushTask();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());
        sessions.add(session);
        
        // 发送连接成功消息
        sendMessage(session, Map.of(
            "type", "CONNECTED",
            "message", "Connected to metrics stream",
            "sessionId", session.getId()
        ));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed: {} - {}", session.getId(), status);
        sessions.remove(session);
        
        // 移除所有订阅
        metricSubscriptions.values().forEach(subscribers -> subscribers.remove(session));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received message from {}: {}", session.getId(), payload);
        
        try {
            Map<String, Object> command = objectMapper.readValue(payload, Map.class);
            String action = (String) command.get("action");
            
            switch (action) {
                case "SUBSCRIBE":
                    handleSubscribe(session, command);
                    break;
                case "UNSUBSCRIBE":
                    handleUnsubscribe(session, command);
                    break;
                case "REFRESH":
                    handleRefresh(session);
                    break;
                default:
                    sendMessage(session, Map.of(
                        "type", "ERROR",
                        "message", "Unknown action: " + action
                    ));
            }
        } catch (Exception e) {
            log.error("Error handling message from {}: {}", session.getId(), e.getMessage());
            sendMessage(session, Map.of(
                "type", "ERROR",
                "message", "Invalid message format"
            ));
        }
    }

    private void handleSubscribe(WebSocketSession session, Map<String, Object> command) {
        List<String> metricCodes = (List<String>) command.get("metrics");
        if (metricCodes == null || metricCodes.isEmpty()) {
            // 订阅所有指标
            metricCodes = List.of("ALL");
        }
        
        for (String metricCode : metricCodes) {
            metricSubscriptions.computeIfAbsent(metricCode, k -> new CopyOnWriteArraySet<>()).add(session);
        }
        
        sendMessage(session, Map.of(
            "type", "SUBSCRIBED",
            "metrics", metricCodes
        ));
        
        // 立即推送一次当前值
        if (metricCodes.contains("ALL")) {
            pushDashboardMetrics(session);
        } else {
            pushMetrics(session, metricCodes);
        }
    }

    private void handleUnsubscribe(WebSocketSession session, Map<String, Object> command) {
        List<String> metricCodes = (List<String>) command.get("metrics");
        if (metricCodes != null) {
            for (String metricCode : metricCodes) {
                Set<WebSocketSession> subscribers = metricSubscriptions.get(metricCode);
                if (subscribers != null) {
                    subscribers.remove(session);
                }
            }
        }
        
        sendMessage(session, Map.of(
            "type", "UNSUBSCRIBED",
            "metrics", metricCodes
        ));
    }

    private void handleRefresh(WebSocketSession session) {
        // 刷新所有订阅的指标
        Set<String> subscribedMetrics = metricSubscriptions.entrySet().stream()
                .filter(entry -> entry.getValue().contains(session))
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
        
        if (subscribedMetrics.contains("ALL")) {
            pushDashboardMetrics(session);
        } else {
            pushMetrics(session, List.copyOf(subscribedMetrics));
        }
    }

    private void startMetricsPushTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                pushMetricsToAllSubscribers();
            } catch (Exception e) {
                log.error("Error pushing metrics: {}", e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS); // 每5秒推送一次
    }

    private void pushMetricsToAllSubscribers() {
        // 推送仪表盘数据给所有订阅了ALL的会话
        Set<WebSocketSession> allSubscribers = metricSubscriptions.get("ALL");
        if (allSubscribers != null && !allSubscribers.isEmpty()) {
            try {
                Map<String, Object> dashboardData = metricsService.getDashboardMetrics().toMap();
                String message = objectMapper.writeValueAsString(Map.of(
                    "type", "DASHBOARD_UPDATE",
                    "timestamp", System.currentTimeMillis(),
                    "data", dashboardData
                ));
                
                for (WebSocketSession session : allSubscribers) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(message));
                    }
                }
            } catch (Exception e) {
                log.error("Error pushing dashboard metrics: {}", e.getMessage());
            }
        }
        
        // 推送特定指标
        metricSubscriptions.forEach((metricCode, subscribers) -> {
            if (!"ALL".equals(metricCode) && !subscribers.isEmpty()) {
                try {
                    MetricValueDTO metric = metricsService.getCurrentMetric(metricCode);
                    String message = objectMapper.writeValueAsString(Map.of(
                        "type", "METRIC_UPDATE",
                        "timestamp", System.currentTimeMillis(),
                        "metric", metric
                    ));
                    
                    for (WebSocketSession session : subscribers) {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(message));
                        }
                    }
                } catch (Exception e) {
                    log.error("Error pushing metric {}: {}", metricCode, e.getMessage());
                }
            }
        });
    }

    private void pushDashboardMetrics(WebSocketSession session) {
        try {
            Map<String, Object> dashboardData = metricsService.getDashboardMetrics().toMap();
            sendMessage(session, Map.of(
                "type", "DASHBOARD_UPDATE",
                "timestamp", System.currentTimeMillis(),
                "data", dashboardData
            ));
        } catch (Exception e) {
            log.error("Error pushing dashboard metrics to {}: {}", session.getId(), e.getMessage());
        }
    }

    private void pushMetrics(WebSocketSession session, List<String> metricCodes) {
        try {
            List<MetricValueDTO> metrics = metricsService.getCurrentMetrics(metricCodes);
            sendMessage(session, Map.of(
                "type", "METRICS_UPDATE",
                "timestamp", System.currentTimeMillis(),
                "metrics", metrics
            ));
        } catch (Exception e) {
            log.error("Error pushing metrics to {}: {}", session.getId(), e.getMessage());
        }
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> message) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.error("Error sending message to {}: {}", session.getId(), e.getMessage());
        }
    }
}
