package cn.aiedge.module.controller;

import cn.aiedge.module.model.SysModule;
import cn.aiedge.module.model.SysModuleVersion;
import cn.aiedge.module.service.ModuleService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
    @SaCheckPermission("system:module:list")
    @Operation(summary = "获取模块列表（分页 + 名称/编码模糊 + 状态筛选）")
    public ResponseEntity<Map<String, Object>> getModuleList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        IPage<SysModule> page = moduleService.pageModules(keyword, status, pageNum, pageSize);
        // 用 HashMap（而非 Map.of）以容忍潜在的 null 值
        Map<String, Object> body = new HashMap<>();
        body.put("records", page.getRecords());
        body.put("total", page.getTotal());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/versions")
    @SaCheckPermission("system:module:list")
    @Operation(summary = "获取模块版本列表（分页 + 模块/状态/版本号筛选）")
    public ResponseEntity<Map<String, Object>> getVersionList(
            @RequestParam(required = false) Long moduleId,
            @RequestParam(required = false) String releaseStatus,
            @RequestParam(required = false) String version,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "50") Integer pageSize) {
        return okPage(moduleService.pageVersions(moduleId, releaseStatus, version, pageNum, pageSize));
    }

    /**
     * 发布记录：与 /versions 同源（都是 sys_module_version）。
     * 本系统无独立「发布单」实体（见《模块发布开发文档》§7.2）—— 发布动作的产物就是一行版本记录，
     * 故两个端点共用同一份查询，仅默认排序口径一致（发布时间倒序、草稿沉底）。
     */
    @GetMapping("/releases")
    @SaCheckPermission("system:module:list")
    @Operation(summary = "获取模块发布记录（分页 + 模块/状态/版本号筛选）")
    public ResponseEntity<Map<String, Object>> getReleaseList(
            @RequestParam(required = false) Long moduleId,
            @RequestParam(required = false) String releaseStatus,
            @RequestParam(required = false) String version,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "50") Integer pageSize) {
        return okPage(moduleService.pageVersions(moduleId, releaseStatus, version, pageNum, pageSize));
    }

    private ResponseEntity<Map<String, Object>> okPage(IPage<?> page) {
        Map<String, Object> body = new HashMap<>();
        body.put("records", page.getRecords());
        body.put("total", page.getTotal());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/usage")
    @SaCheckPermission("system:module:list")
    @Operation(summary = "获取模块使用统计（含汇总卡；days=统计窗口天数，默认 30）")
    public ResponseEntity<Map<String, Object>> getUsageStats(
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        return ResponseEntity.ok(moduleService.getUsageStats(days));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("system:module:list")
    @Operation(summary = "根据ID获取模块")
    public ResponseEntity<Map<String, Object>> getModuleById(@PathVariable Long id) {
        SysModule module = moduleService.getModuleById(id);
        if (module == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "模块不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", module));
    }

    @PostMapping
    @SaCheckPermission("system:module:create")
    @Operation(summary = "创建模块")
    public ResponseEntity<Map<String, Object>> createModule(@RequestBody SysModule module) {
        SysModule created = moduleService.createModule(module);
        return ResponseEntity.ok(Map.of("success", true, "data", created, "message", "创建成功"));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("system:module:update")
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
    @SaCheckPermission("system:module:delete")
    @Operation(summary = "删除模块")
    public ResponseEntity<Map<String, Object>> deleteModule(@PathVariable Long id) {
        boolean success = moduleService.deleteModule(id);
        return ResponseEntity.ok(Map.of("success", success, "message", success ? "删除成功" : "模块不存在"));
    }

    @PutMapping("/{id}/status")
    @SaCheckPermission("system:module:update")
    @Operation(summary = "切换模块状态")
    public ResponseEntity<Map<String, Object>> toggleStatus(@PathVariable Long id) {
        boolean success = moduleService.toggleStatus(id);
        if (!success) {
            return ResponseEntity.ok(Map.of("success", false, "message", "模块不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "状态切换成功"));
    }

    @PostMapping("/{id}/publish")
    @SaCheckPermission("system:module:update")
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

    /**
     * 回滚：把模块当前版本号回写为某个已存在的历史版本（不新增版本行）。
     * 说明：这是**本系统设计**（业界对标未取得「按版本回滚」的官方正文，见《模块发布开发文档》§8），
     * 语义限定为「切换当前版本指针」，不做代码/数据层面的真实回退。
     */
    @PostMapping("/{id}/rollback")
    @SaCheckPermission("system:module:update")
    @Operation(summary = "回滚模块当前版本")
    public ResponseEntity<Map<String, Object>> rollbackVersion(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String version = request.get("version");
        if (version == null || version.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "版本号不能为空"));
        }
        boolean success = moduleService.rollbackVersion(id, version);
        if (!success) {
            return ResponseEntity.ok(Map.of("success", false, "message", "模块或目标版本不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "已回滚到 " + version));
    }
}
