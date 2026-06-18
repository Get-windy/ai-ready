package cn.aiedge.tenant.controller;

import cn.aiedge.tenant.model.SysTenantPackage;
import cn.aiedge.tenant.service.TenantPackageService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 租户套餐控制器
 */
@RestController
@RequestMapping("/api/tenant-package")
@RequiredArgsConstructor
@Tag(name = "租户套餐管理", description = "租户套餐CRUD")
public class TenantPackageController {

    private final TenantPackageService tenantPackageService;

    @GetMapping("/list")
    @SaCheckPermission("platform:tenant-package:list")
    @Operation(summary = "获取套餐列表")
    public ResponseEntity<Map<String, Object>> getList() {
        List<SysTenantPackage> list = tenantPackageService.getList();
        return ResponseEntity.ok(Map.of("records", list, "total", list.size()));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("platform:tenant-package:list")
    @Operation(summary = "获取套餐详情")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        SysTenantPackage pkg = tenantPackageService.getById(id);
        if (pkg == null) {
            return ResponseEntity.ok(Map.of("code", 200, "data", null, "message", "套餐不存在"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "data", pkg, "message", "ok"));
    }

    @PostMapping
    @SaCheckPermission("platform:tenant-package:create")
    @Operation(summary = "创建套餐")
    public ResponseEntity<Map<String, Object>> create(@RequestBody SysTenantPackage pkg) {
        SysTenantPackage created = tenantPackageService.create(pkg);
        return ResponseEntity.ok(Map.of("code", 200, "data", created, "message", "创建成功"));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("platform:tenant-package:update")
    @Operation(summary = "更新套餐")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody SysTenantPackage pkg) {
        pkg.setId(id);
        SysTenantPackage updated = tenantPackageService.update(pkg);
        if (updated == null) {
            return ResponseEntity.ok(Map.of("code", 200, "data", null, "message", "套餐不存在"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "data", updated, "message", "更新成功"));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("platform:tenant-package:delete")
    @Operation(summary = "删除套餐")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        boolean success = tenantPackageService.delete(id);
        return ResponseEntity.ok(Map.of("code", 200, "data", success, "message", success ? "删除成功" : "套餐不存在"));
    }
}
