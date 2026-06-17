package cn.aiedge.platform.controller;

import cn.aiedge.platform.model.SmsConfig;
import cn.aiedge.platform.service.SmsConfigService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 短信配置控制器
 */
@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
@Tag(name = "短信配置", description = "短信服务配置管理")
public class SmsConfigController {

    private final SmsConfigService smsConfigService;

    @GetMapping("/config")
    @SaCheckPermission("platform:sms:config")
    @Operation(summary = "获取短信配置")
    public ResponseEntity<Map<String, Object>> getConfig(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        SmsConfig config = smsConfigService.getConfig(tenantId);
        return ResponseEntity.ok(Map.of("code", 200, "data", config, "message", "ok"));
    }

    @PostMapping("/config")
    @SaCheckPermission("platform:sms:update")
    @Operation(summary = "保存短信配置")
    public ResponseEntity<Map<String, Object>> saveConfig(
            @RequestBody SmsConfig config,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        SmsConfig saved = smsConfigService.saveConfig(config, tenantId);
        return ResponseEntity.ok(Map.of("success", true, "config", saved));
    }

    @PostMapping("/test")
    @SaCheckPermission("platform:sms:test")
    @Operation(summary = "测试短信服务")
    public ResponseEntity<Map<String, Object>> testConnection(
            @RequestBody SmsConfig config) {
        boolean success = smsConfigService.testConnection(config);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "短信服务测试成功" : "短信服务测试失败"
        ));
    }
}
