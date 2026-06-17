package cn.aiedge.platform.controller;

import cn.aiedge.platform.model.SecurityPolicy;
import cn.aiedge.platform.service.SecurityPolicyService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 安全策略控制器
 */
@RestController
@RequestMapping("/api/system/security/policy")
@RequiredArgsConstructor
@Tag(name = "安全策略", description = "系统安全策略配置管理")
public class SecurityPolicyController {

    private final SecurityPolicyService securityPolicyService;

    @GetMapping
    @SaCheckPermission("platform:security:policy")
    @Operation(summary = "获取安全策略")
    public ResponseEntity<Map<String, Object>> getPolicy(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        SecurityPolicy policy = securityPolicyService.getPolicy(tenantId);
        return ResponseEntity.ok(Map.of("code", 200, "data", policy, "message", "ok"));
    }

    @PostMapping("/save")
    @SaCheckPermission("platform:security:update")
    @Operation(summary = "保存安全策略")
    public ResponseEntity<Map<String, Object>> savePolicy(
            @RequestBody SecurityPolicy policy,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        SecurityPolicy saved = securityPolicyService.savePolicy(policy, tenantId);
        return ResponseEntity.ok(Map.of("code", 200, "data", saved, "message", "保存成功"));
    }
}
