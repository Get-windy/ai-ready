package cn.aiedge.module.controller;

import cn.aiedge.module.model.SysModule;
import cn.aiedge.module.model.SysModuleVersion;
import cn.aiedge.module.service.ModuleService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模块管理控制器
 */
@RestController
@RequestMapping("/api/module")
@RequiredArgsConstructor
@Tag(name = "模块管理", description = "系统模块管理功能")
public class ModuleController {

    private final ModuleService moduleService;

    @GetMapping("/list")
    @SaCheckPermission("module:list")
    @Operation(summary = "获取模块列表")
    public ResponseEntity<Map<String, Object>> getModuleList() {
        List<SysModule> modules = moduleService.getModuleList();
        return ResponseEntity.ok(Map.of("records", modules, "total", modules.size()));
    }

    @GetMapping("/versions")
    @SaCheckPermission("module:list")
    @Operation(summary = "获取模块版本列表")
    public ResponseEntity<Map<String, Object>> getVersionList(
            @RequestParam(required = false) Long moduleId) {
        List<SysModuleVersion> versions = moduleService.getVersionList();
        if (moduleId != null) {
            versions = versions.stream()
                    .filter(v -> moduleId.equals(v.getModuleId()))
                    .toList();
        }
        return ResponseEntity.ok(Map.of("records", versions, "total", versions.size()));
    }

    @GetMapping("/releases")
    @SaCheckPermission("module:list")
    @Operation(summary = "获取模块发布记录")
    public ResponseEntity<Map<String, Object>> getReleaseList(
            @RequestParam(required = false) Long moduleId) {
        List<SysModuleVersion> versions = moduleService.getVersionList();
        if (moduleId != null) {
            versions = versions.stream()
                    .filter(v -> moduleId.equals(v.getModuleId()))
                    .toList();
        }
        return ResponseEntity.ok(Map.of("records", versions, "total", versions.size()));
    }

    @GetMapping("/usage")
    @SaCheckPermission("module:list")
    @Operation(summary = "获取模块使用统计")
    public ResponseEntity<Map<String, Object>> getUsageStats() {
        List<Map<String, Object>> stats = moduleService.getUsageStats();
        return ResponseEntity.ok(Map.of("records", stats, "total", stats.size()));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("module:list")
    @Operation(summary = "根据ID获取模块")
    public ResponseEntity<Map<String, Object>> getModuleById(@PathVariable Long id) {
        SysModule module = moduleService.getModuleById(id);
        if (module == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "模块不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", module));
    }

    @PostMapping
    @SaCheckPermission("module:create")
    @Operation(summary = "创建模块")
    public ResponseEntity<Map<String, Object>> createModule(@RequestBody SysModule module) {
        SysModule created = moduleService.createModule(module);
        return ResponseEntity.ok(Map.of("success", true, "data", created, "message", "创建成功"));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("module:update")
    @Operation(summary = "更新模块")
    public ResponseEntity<Map<String, Object>> updateModule(
            @PathVariable Long id,
            @RequestBody SysModule module) {
        module.setId(id);
        SysModule updated = moduleService.updateModule(module);
        if (updated == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "模块不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", updated, "message", "更新成功"));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("module:delete")
    @Operation(summary = "删除模块")
    public ResponseEntity<Map<String, Object>> deleteModule(@PathVariable Long id) {
        boolean success = moduleService.deleteModule(id);
        return ResponseEntity.ok(Map.of("success", success, "message", success ? "删除成功" : "模块不存在"));
    }

    @PutMapping("/{id}/status")
    @SaCheckPermission("module:update")
    @Operation(summary = "切换模块状态")
    public ResponseEntity<Map<String, Object>> toggleStatus(@PathVariable Long id) {
        boolean success = moduleService.toggleStatus(id);
        if (!success) {
            return ResponseEntity.ok(Map.of("success", false, "message", "模块不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "状态切换成功"));
    }

    @PostMapping("/{id}/publish")
    @SaCheckPermission("module:update")
    @Operation(summary = "发布新版本")
    public ResponseEntity<Map<String, Object>> publishVersion(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String version = request.get("version");
        String changelog = request.get("changelog");
        if (version == null || version.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "版本号不能为空"));
        }
        SysModuleVersion v = moduleService.publishVersion(id, version, changelog);
        if (v == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "模块不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", v, "message", "发布成功"));
    }
}
