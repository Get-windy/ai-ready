package cn.aiedge.platform.controller;

import cn.aiedge.platform.model.MailConfig;
import cn.aiedge.platform.service.MailConfigService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 邮件配置控制器
 */
@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
@Tag(name = "邮件配置", description = "邮件服务配置管理")
public class MailConfigController {

    private final MailConfigService mailConfigService;

    @GetMapping("/config")
    @SaCheckPermission("platform:mail:config")
    @Operation(summary = "获取邮件配置")
    public ResponseEntity<Map<String, Object>> getConfig(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        MailConfig config = mailConfigService.getConfig(tenantId);
        return ResponseEntity.ok(Map.of("code", 200, "data", config, "message", "ok"));
    }

    @PostMapping("/config")
    @SaCheckPermission("platform:mail:update")
    @Operation(summary = "保存邮件配置")
    public ResponseEntity<Map<String, Object>> saveConfig(
            @RequestBody MailConfig config,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        MailConfig saved = mailConfigService.saveConfig(config, tenantId);
        return ResponseEntity.ok(Map.of("success", true, "config", saved));
    }

    @PostMapping("/test")
    @SaCheckPermission("platform:mail:test")
    @Operation(summary = "测试SMTP连接")
    public ResponseEntity<Map<String, Object>> testConnection(
            @RequestBody MailConfig config) {
        boolean success = mailConfigService.testConnection(config);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "SMTP连接测试成功" : "SMTP连接测试失败"
        ));
    }
}
