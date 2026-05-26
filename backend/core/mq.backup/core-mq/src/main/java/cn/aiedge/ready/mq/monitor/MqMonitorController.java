package cn.aiedge.ready.mq.monitor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息队列监控API控制器
 */
@RestController
@RequestMapping("/api/mq/monitor")
public class MqMonitorController {

    @Autowired
    private MqMetricsService mqMetricsService;

    /**
     * 获取所有队列状态
     */
    @GetMapping("/queues")
    public ResponseEntity<Map<String, Object>> getAllQueuesStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", mqMetricsService.getAllQueuesStatus());
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * 获取指定队列状态
     */
    @GetMapping("/queues/{queueName}")
    public ResponseEntity<Map<String, Object>> getQueueStatus(@PathVariable String queueName) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", mqMetricsService.getQueueInfo(queueName));
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * 获取监控指标概览
     */
    @GetMapping("/metrics/overview")
    public ResponseEntity<Map<String, Object>> getMetricsOverview() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("queues", mqMetricsService.getAllQueuesStatus());
        overview.put("timestamp", System.currentTimeMillis());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", overview);
        return ResponseEntity.ok(response);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("component", "MQ-Monitor");
        health.put("timestamp", System.currentTimeMillis());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", health);
        return ResponseEntity.ok(response);
    }
}
