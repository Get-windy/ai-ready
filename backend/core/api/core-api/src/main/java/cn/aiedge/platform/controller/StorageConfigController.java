package cn.aiedge.platform.controller;

import cn.aiedge.platform.dto.ConnectionTestResult;
import cn.aiedge.platform.model.StorageConfig;
import cn.aiedge.platform.service.StorageConfigService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 存储配置控制器
 */
@RestController
@RequestMapping("/api/storage-config")
@RequiredArgsConstructor
@Tag(name = "存储配置", description = "存储服务配置管理")
public class StorageConfigController {

    private final StorageConfigService storageConfigService;

    @GetMapping("/config")
    @SaCheckPermission("platform:storage:config")
    @Operation(summary = "获取存储配置")
    public ResponseEntity<Map<String, Object>> getConfig() {
        StorageConfig config = storageConfigService.getConfig(0L);
        return ResponseEntity.ok(Map.of("code", 200, "data", config, "message", "ok"));
    }

    @PostMapping("/config")
    @SaCheckPermission("platform:storage:update")
    @Operation(summary = "保存存储配置")
    public ResponseEntity<Map<String, Object>> saveConfig(
            @RequestBody StorageConfig config) {
        StorageConfig saved = storageConfigService.saveConfig(config, 0L);
        return ResponseEntity.ok(Map.of("success", true, "config", saved));
    }

    @PostMapping("/test")
    @SaCheckPermission("platform:storage:test")
    @Operation(summary = "测试存储连接")
    public ResponseEntity<Map<String, Object>> testConnection(
            @RequestBody StorageConfig config) {
        // 真实测试：返回布尔 + 人话原因（此前只 return true，页面永远显示成功）
        ConnectionTestResult result = storageConfigService.testConnection(config);
        return ResponseEntity.ok(Map.of(
                "success", result.success(),
                "message", result.message()
        ));
    }
}
