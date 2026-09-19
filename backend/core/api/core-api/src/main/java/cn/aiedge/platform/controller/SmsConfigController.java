package cn.aiedge.platform.controller;

import cn.aiedge.platform.dto.ConnectionTestResult;
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
    public ResponseEntity<Map<String, Object>> getConfig() {
        SmsConfig config = smsConfigService.getConfig(0L);
        return ResponseEntity.ok(Map.of("code", 200, "data", config, "message", "ok"));
    }

    @PostMapping("/config")
    @SaCheckPermission("platform:sms:update")
    @Operation(summary = "保存短信配置")
    public ResponseEntity<Map<String, Object>> saveConfig(
            @RequestBody SmsConfig config) {
        SmsConfig saved = smsConfigService.saveConfig(config, 0L);
        return ResponseEntity.ok(Map.of("success", true, "config", saved));
    }

    @PostMapping("/test")
    @SaCheckPermission("platform:sms:test")
    @Operation(summary = "测试短信服务")
    public ResponseEntity<Map<String, Object>> testConnection(
            @RequestBody SmsConfig config) {
        // 真实测试：返回布尔 + 人话原因（此前只 return true，页面永远显示成功）
        ConnectionTestResult result = smsConfigService.testConnection(config);
        return ResponseEntity.ok(Map.of(
                "success", result.success(),
                "message", result.message()
        ));
    }
}
