package cn.aiedge.platform.controller;

import cn.aiedge.platform.dto.ConnectionTestResult;
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
    public ResponseEntity<Map<String, Object>> getConfig() {
        MailConfig config = mailConfigService.getConfig(0L);
        return ResponseEntity.ok(Map.of("code", 200, "data", config, "message", "ok"));
    }

    @PostMapping("/config")
    @SaCheckPermission("platform:mail:update")
    @Operation(summary = "保存邮件配置")
    public ResponseEntity<Map<String, Object>> saveConfig(
            @RequestBody MailConfig config) {
        MailConfig saved = mailConfigService.saveConfig(config, 0L);
        return ResponseEntity.ok(Map.of("success", true, "config", saved));
    }

    @PostMapping("/test")
    @SaCheckPermission("platform:mail:test")
    @Operation(summary = "测试SMTP连接")
    public ResponseEntity<Map<String, Object>> testConnection(
            @RequestBody MailConfig config) {
        // 真实测试：返回布尔 + 人话原因（此前只 return true，页面永远显示成功）
        ConnectionTestResult result = mailConfigService.testConnection(config);
        return ResponseEntity.ok(Map.of(
                "success", result.success(),
                "message", result.message()
        ));
    }
}
