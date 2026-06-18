package cn.aiedge.tenant.controller;

import cn.aiedge.tenant.model.SysTenantQuota;
import cn.aiedge.tenant.service.TenantQuotaService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 租户配额控制器
 */
@RestController
@RequestMapping("/api/tenant-quota")
@RequiredArgsConstructor
@Tag(name = "租户配额管理", description = "租户配额CRUD")
public class TenantQuotaController {

    private final TenantQuotaService tenantQuotaService;

    @GetMapping("/list")
    @SaCheckPermission("platform:tenant-quota:list")
    @Operation(summary = "获取配额列表")
    public ResponseEntity<Map<String, Object>> getList() {
        List<SysTenantQuota> list = tenantQuotaService.getList();
        return ResponseEntity.ok(Map.of("records", list, "total", list.size()));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("platform:tenant-quota:list")
    @Operation(summary = "获取配额详情")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        SysTenantQuota quota = tenantQuotaService.getById(id);
        if (quota == null) {
            return ResponseEntity.ok(Map.of("code", 200, "data", null, "message", "配额不存在"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "data", quota, "message", "ok"));
    }

    @PostMapping
    @SaCheckPermission("platform:tenant-quota:create")
    @Operation(summary = "创建配额")
    public ResponseEntity<Map<String, Object>> create(@RequestBody SysTenantQuota quota) {
        SysTenantQuota created = tenantQuotaService.create(quota);
        return ResponseEntity.ok(Map.of("code", 200, "data", created, "message", "创建成功"));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("platform:tenant-quota:update")
    @Operation(summary = "更新配额")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody SysTenantQuota quota) {
        quota.setId(id);
        SysTenantQuota updated = tenantQuotaService.update(quota);
        if (updated == null) {
            return ResponseEntity.ok(Map.of("code", 200, "data", null, "message", "配额不存在"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "data", updated, "message", "更新成功"));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("platform:tenant-quota:delete")
    @Operation(summary = "删除配额")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        boolean success = tenantQuotaService.delete(id);
        return ResponseEntity.ok(Map.of("code", 200, "data", success, "message", success ? "删除成功" : "配额不存在"));
    }
}
