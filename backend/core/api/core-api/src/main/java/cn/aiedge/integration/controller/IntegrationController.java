package cn.aiedge.integration.controller;

import cn.aiedge.integration.model.*;
import cn.aiedge.integration.service.IntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统集成控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/integration")
@RequiredArgsConstructor
@Tag(name = "系统集成", description = "第三方系统集成和数据同步")
public class IntegrationController {

    private final IntegrationService integrationService;

    // ==================== 集成配置管理 ====================

    @PostMapping("/configs")
    @Operation(summary = "创建集成配置")
    public ResponseEntity<IntegrationConfig> createConfig(@RequestBody IntegrationConfig config) {
        IntegrationConfig saved = integrationService.createConfig(config);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/configs/{configId}")
    @Operation(summary = "获取集成配置")
    public ResponseEntity<IntegrationConfig> getConfig(@PathVariable String configId) {
        IntegrationConfig config = integrationService.getConfig(configId);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }

    @GetMapping("/configs/system/{systemCode}")
    @Operation(summary = "根据系统编码获取配置")
    public ResponseEntity<IntegrationConfig> getConfigBySystemCode(@PathVariable String systemCode) {
        IntegrationConfig config = integrationService.getConfigBySystemCode(systemCode);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }

    @PutMapping("/configs/{configId}")
    @Operation(summary = "更新集成配置")
    public ResponseEntity<IntegrationConfig> updateConfig(
            @PathVariable String configId,
            @RequestBody IntegrationConfig config) {
        IntegrationConfig updated = integrationService.updateConfig(configId, config);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/configs/{configId}")
    @Operation(summary = "删除集成配置")
    public ResponseEntity<Map<String, Object>> deleteConfig(@PathVariable String configId) {
        boolean success = integrationService.deleteConfig(configId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/configs")
    @Operation(summary = "获取所有配置")
    public ResponseEntity<List<IntegrationConfig>> listConfigs() {
        List<IntegrationConfig> configs = integrationService.listConfigs();
        return ResponseEntity.ok(configs);
    }

    @PostMapping("/configs/{configId}/toggle")
    @Operation(summary = "启用/禁用配置")
    public ResponseEntity<Map<String, Object>> toggleConfig(
            @PathVariable String configId,
            @RequestParam boolean enabled) {
        boolean success = integrationService.toggleConfigStatus(configId, enabled);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("status", enabled ? "enabled" : "disabled");
        return ResponseEntity.ok(result);
    }

    // ==================== 用户数据同步 ====================

    @PostMapping("/sync/user")
    @Operation(summary = "同步单个用户")
    public ResponseEntity<SyncRecord> syncUser(
            @RequestParam String configId,
            @RequestParam String operation,
            @RequestBody Map<String, Object> userData) {
        SyncRecord record = integrationService.syncUser(configId, operation, userData);
        return ResponseEntity.ok(record);
    }

    @PostMapping("/sync/users")
    @Operation(summary = "批量同步用户")
    public ResponseEntity<List<SyncRecord>> syncUsers(
            @RequestParam String configId,
            @RequestParam String operation,
            @RequestBody List<Map<String, Object>> users) {
        List<SyncRecord> records = integrationService.syncUsers(configId, operation, users);
        return ResponseEntity.ok(records);
    }

    // ==================== 订单数据同步 ====================

    @PostMapping("/sync/order")
    @Operation(summary = "同步单个订单")
    public ResponseEntity<SyncRecord> syncOrder(
            @RequestParam String configId,
            @RequestParam String operation,
            @RequestBody Map<String, Object> orderData) {
        SyncRecord record = integrationService.syncOrder(configId, operation, orderData);
        return ResponseEntity.ok(record);
    }

    @PostMapping("/sync/orders")
    @Operation(summary = "批量同步订单")
    public ResponseEntity<List<SyncRecord>> syncOrders(
            @RequestParam String configId,
            @RequestParam String operation,
            @RequestBody List<Map<String, Object>> orders) {
        List<SyncRecord> records = integrationService.syncOrders(configId, operation, orders);
        return ResponseEntity.ok(records);
    }

    // ==================== 产品数据同步 ====================

    @PostMapping("/sync/product")
    @Operation(summary = "同步产品")
    public ResponseEntity<SyncRecord> syncProduct(
            @RequestParam String configId,
            @RequestParam String operation,
            @RequestBody Map<String, Object> productData) {
        SyncRecord record = integrationService.syncProduct(configId, operation, productData);
        return ResponseEntity.ok(record);
    }

    // ==================== 全量/增量同步 ====================

    @PostMapping("/sync/full")
    @Operation(summary = "全量同步")
    public ResponseEntity<Map<String, Object>> fullSync(
            @RequestParam String configId,
            @RequestParam String syncType) {
        Map<String, Object> result = integrationService.fullSync(configId, syncType);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sync/incremental")
    @Operation(summary = "增量同步")
    public ResponseEntity<Map<String, Object>> incrementalSync(
            @RequestParam String configId,
            @RequestParam String syncType,
            @RequestParam String lastSyncTime) {
        Map<String, Object> result = integrationService.incrementalSync(configId, syncType, lastSyncTime);
        return ResponseEntity.ok(result);
    }

    // ==================== 同步记录查询 ====================

    @GetMapping("/sync/records/{recordId}")
    @Operation(summary = "获取同步记录")
    public ResponseEntity<SyncRecord> getSyncRecord(@PathVariable String recordId) {
        SyncRecord record = integrationService.getSyncRecord(recordId);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(record);
    }

    @GetMapping("/sync/records")
    @Operation(summary = "查询同步记录")
    public ResponseEntity<List<SyncRecord>> listSyncRecords(
            @RequestParam String configId,
            @RequestParam(required = false) String syncType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        List<SyncRecord> records = integrationService.listSyncRecords(configId, syncType, status, page, pageSize);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/sync/statistics")
    @Operation(summary = "获取同步统计")
    public ResponseEntity<Map<String, Object>> getSyncStatistics(@RequestParam String configId) {
        Map<String, Object> stats = integrationService.getSyncStatistics(configId);
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/sync/records/{recordId}/retry")
    @Operation(summary = "重试同步")
    public ResponseEntity<Map<String, Object>> retrySync(@PathVariable String recordId) {
        boolean success = integrationService.retrySync(recordId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        return ResponseEntity.ok(result);
    }

    // ==================== WebHook ====================

    @PostMapping("/webhook/{configId}")
    @Operation(summary = "接收WebHook")
    public ResponseEntity<Map<String, Object>> receiveWebhook(
            @PathVariable String configId,
            @RequestBody Map<String, Object> data) {
        log.info("接收WebHook: configId={}", configId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "WebHook接收成功");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/webhook/send/{configId}")
    @Operation(summary = "发送WebHook")
    public ResponseEntity<Map<String, Object>> sendWebhook(
            @PathVariable String configId,
            @RequestParam String eventType,
            @RequestBody Object data) {
        boolean success = integrationService.sendWebhook(configId, eventType, data);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        return ResponseEntity.ok(result);
    }

    // ==================== 健康检查 ====================

    @GetMapping("/health/{configId}")
    @Operation(summary = "健康检查")
    public ResponseEntity<Map<String, Object>> checkHealth(@PathVariable String configId) {
        Map<String, Object> health = integrationService.checkHealth(configId);
        return ResponseEntity.ok(health);
    }

    @PostMapping("/test/{configId}")
    @Operation(summary = "测试连接")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable String configId) {
        boolean connected = integrationService.testConnection(configId);
        Map<String, Object> result = new HashMap<>();
        result.put("configId", configId);
        result.put("connected", connected);
        return ResponseEntity.ok(result);
    }
}
