package cn.aiedge.config.controller;

import cn.aiedge.config.model.ConfigChangeLog;
import cn.aiedge.config.model.SystemConfig;
import cn.aiedge.config.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置控制器
 */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@Tag(name = "系统配置", description = "系统配置管理功能")
public class SystemConfigController {

    private final SystemConfigService configService;

    @GetMapping("/list")
    @Operation(summary = "获取配置列表")
    public ResponseEntity<Map<String, Object>> getConfigList(
            @RequestParam(required = false) String configType,
            @RequestParam(required = false) String configGroup,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        List<SystemConfig> configs = configService.getConfigList(configType, configGroup, tenantId);
        return ResponseEntity.ok(Map.of("configs", configs, "total", configs.size()));
    }

    @GetMapping("/map")
    @Operation(summary = "获取配置Map")
    public ResponseEntity<Map<String, String>> getConfigMap(
            @RequestParam(required = false) String configGroup,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        Map<String, String> configMap = configService.getConfigMap(configGroup, tenantId);
        return ResponseEntity.ok(configMap);
    }

    @GetMapping("/value/{configKey}")
    @Operation(summary = "获取配置值")
    public ResponseEntity<Map<String, Object>> getConfigValue(
            @PathVariable String configKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        SystemConfig config = configService.getConfigByKey(configKey, tenantId);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("key", configKey, "value", config.getConfigValue(), "config", config));
    }

    @PostMapping("/save")
    @Operation(summary = "保存配置")
    public ResponseEntity<Map<String, Object>> saveConfig(
            @RequestBody SystemConfig config,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        SystemConfig saved = configService.saveConfig(config, tenantId);
        return ResponseEntity.ok(Map.of("success", true, "config", saved));
    }

    @PostMapping("/save-value")
    @Operation(summary = "保存配置值")
    public ResponseEntity<Map<String, Object>> saveConfigValue(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        String configKey = request.get("configKey");
        String configValue = request.get("configValue");
        configService.saveConfigValue(configKey, configValue, tenantId);
        return ResponseEntity.ok(Map.of("success", true, "message", "保存成功"));
    }

    @PostMapping("/batch-save")
    @Operation(summary = "批量保存配置")
    public ResponseEntity<Map<String, Object>> batchSaveConfigs(
            @RequestBody Map<String, String> configs,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        configService.batchSaveConfigs(configs, tenantId);
        return ResponseEntity.ok(Map.of("success", true, "count", configs.size()));
    }

    @DeleteMapping("/{configKey}")
    @Operation(summary = "删除配置")
    public ResponseEntity<Map<String, Object>> deleteConfig(
            @PathVariable String configKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        boolean success = configService.deleteConfigByKey(configKey, tenantId);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @GetMapping("/logs/{configKey}")
    @Operation(summary = "获取配置变更日志")
    public ResponseEntity<List<ConfigChangeLog>> getConfigChangeLogs(
            @PathVariable String configKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        List<ConfigChangeLog> logs = configService.getConfigChangeLogs(configKey, tenantId);
        return ResponseEntity.ok(logs);
    }

    @PostMapping("/refresh-cache")
    @Operation(summary = "刷新配置缓存")
    public ResponseEntity<Map<String, Object>> refreshCache(
            @RequestParam(required = false) String configKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        if (configKey != null) {
            configService.refreshCache(configKey, tenantId);
        } else {
            configService.refreshCache(tenantId);
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "缓存刷新成功"));
    }

    @GetMapping("/types")
    @Operation(summary = "获取配置类型")
    public ResponseEntity<List<Map<String, String>>> getConfigTypes() {
        return ResponseEntity.ok(List.of(
            Map.of("code", "system", "name", "系统配置"),
            Map.of("code", "security", "name", "安全配置"),
            Map.of("code", "business", "name", "业务配置"),
            Map.of("code", "notification", "name", "通知配置"),
            Map.of("code", "integration", "name", "集成配置")
        ));
    }

    @GetMapping("/groups")
    @Operation(summary = "获取配置分组")
    public ResponseEntity<List<Map<String, String>>> getConfigGroups() {
        return ResponseEntity.ok(List.of(
            Map.of("code", "basic", "name", "基础配置"),
            Map.of("code", "login", "name", "登录配置"),
            Map.of("code", "password", "name", "密码配置"),
            Map.of("code", "session", "name", "会话配置"),
            Map.of("code", "upload", "name", "上传配置"),
            Map.of("code", "email", "name", "邮件配置"),
            Map.of("code", "sms", "name", "短信配置")
        ));
    }
}
