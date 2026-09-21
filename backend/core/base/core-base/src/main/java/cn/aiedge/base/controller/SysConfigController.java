package cn.aiedge.base.controller;

import cn.aiedge.base.entity.SysProjectConfig;
import cn.aiedge.base.service.SysConfigService;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统配置管理控制器
 *
 * <p><b>⚠️ 2026-09-21 登记：本类没有前端调用方，且与 {@code SystemConfigController} 共用一套权限码。</b></p>
 *
 * <p>两个类都叫「系统配置」，但读写的是**不同的表**：</p>
 * <ul>
 *   <li>本类（{@code /api/system/config}，12 端点 + history / rollback / compare）读写
 *       {@code sys_project_config}（经 {@code SysConfigServiceImpl}）；</li>
 *   <li>{@code cn.aiedge.config.controller.SystemConfigController}（{@code /api/config}）读写 {@code sys_config}，
 *       且它才是 {@code views/system/config/index.vue} 与 {@code views/set/sys-params/index.vue}
 *       实际调用的那一套（前端 {@code api/config.ts} 全部指向 {@code /config/*}）。</li>
 * </ul>
 *
 * <p>两者共用 {@code system:config:list|update|delete|export} 这套权限码，而
 * {@code sys_permission.api_path} 每个码只有一列 ⇒ 库里这几条码的 {@code api_path} 记的是
 * {@code /api/config/*}（有调用方的那一个）。因此 {@code PermissionServiceImpl.checkApiPermission}
 * 拿 {@code /api/system/config/*} 反查永远命中不到。</p>
 *
 * <p><b>这不是 {@code api_path} 填错了</b> —— 它描述的正是唯一有调用方的控制器；
 * 真正的问题是「同一套码被两个控制器共用」。处置（删掉本类，还是把它的
 * history/rollback/compare 接到活页面上）属**待拍板项**，见 {@code MASTER_TODO_20260920.md}。
 * 在拍板前**不要**给本类单独造一套新权限码 —— 那等于给没有调用方的接口造码。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/system/config")
@RequiredArgsConstructor
@Tag(name = "系统配置管理", description = "配置增删改查、热更新、版本管理")
public class SysConfigController {

    private final SysConfigService configService;

    // ==================== 配置查询 ====================

    @GetMapping("/value/{key}")
    @Operation(summary = "获取配置值")
    @SaCheckPermission("system:config:list")
    public Result<String> getValue(@PathVariable String key) {
        return Result.ok(configService.getValue(key, null));
    }

    @GetMapping("/get/{key}")
    @Operation(summary = "获取配置对象")
    @SaCheckPermission("system:config:list")
    public Result<SysProjectConfig> getConfig(@PathVariable String key) {
        return Result.ok(configService.getConfig(key).orElse(null));
    }

    @GetMapping("/group/{group}")
    @Operation(summary = "获取配置分组")
    @SaCheckPermission("system:config:list")
    public Result<List<SysProjectConfig>> getConfigsByGroup(@PathVariable String group) {
        return Result.ok(configService.getConfigsByGroup(group));
    }

    @GetMapping("/list")
    @Operation(summary = "获取所有配置")
    @SaCheckPermission("system:config:list")
    public Result<List<SysProjectConfig>> getAllConfigs() {
        return Result.ok(configService.getAllConfigs());
    }

    @GetMapping("/map")
    @Operation(summary = "获取配置Map")
    @SaCheckPermission("system:config:list")
    public Result<Map<String, String>> getConfigMap() {
        return Result.ok(configService.getConfigMap());
    }

    // ==================== 配置更新 ====================

    @PostMapping("/set")
    @Operation(summary = "设置配置")
    @SaCheckPermission("system:config:update")
    public Result<Void> setValue(@RequestBody ConfigRequest request) {
        configService.setValue(
            request.getKey(), 
            request.getValue(),
            request.getType(),
            request.getGroup(),
            request.getDescription()
        );
        return Result.ok();
    }

    @PostMapping("/batch")
    @Operation(summary = "批量设置配置")
    @SaCheckPermission("system:config:update")
    public Result<Void> setValues(@RequestBody Map<String, String> configs) {
        configService.setValues(configs);
        return Result.ok();
    }

    @DeleteMapping("/{key}")
    @Operation(summary = "删除配置")
    @SaCheckPermission("system:config:delete")
    public Result<Void> deleteConfig(@PathVariable String key) {
        configService.deleteConfig(key);
        return Result.ok();
    }

    // ==================== 配置热更新 ====================

    @PostMapping("/refresh")
    @Operation(summary = "刷新所有配置缓存")
    @SaCheckPermission("system:config:update")
    public Result<Void> refreshCache() {
        configService.refreshCache();
        return Result.ok();
    }

    @PostMapping("/refresh/{key}")
    @Operation(summary = "刷新指定配置")
    @SaCheckPermission("system:config:update")
    public Result<Void> refreshConfig(@PathVariable String key) {
        configService.refreshConfig(key);
        return Result.ok();
    }

    // ==================== 配置版本管理 ====================

    @GetMapping("/history/{key}")
    @Operation(summary = "获取配置历史")
    @SaCheckPermission("system:config:list")
    public Result<List<SysConfigService.ConfigHistory>> getConfigHistory(@PathVariable String key) {
        return Result.ok(configService.getConfigHistory(key));
    }

    @PostMapping("/rollback")
    @Operation(summary = "回滚配置")
    @SaCheckPermission("system:config:update")
    public Result<Void> rollbackConfig(@RequestBody RollbackRequest request) {
        configService.rollbackConfig(request.getKey(), request.getVersion());
        return Result.ok();
    }

    @GetMapping("/compare")
    @Operation(summary = "比较配置版本")
    @SaCheckPermission("system:config:list")
    public Result<SysConfigService.ConfigDiff> compareVersions(
            @RequestParam String key,
            @RequestParam Long version1,
            @RequestParam Long version2) {
        return Result.ok(configService.compareVersions(key, version1, version2));
    }

    // ==================== 请求DTO ====================

    @lombok.Data
    public static class ConfigRequest {
        private String key;
        private String value;
        private String type;
        private String group;
        private String description;
    }

    @lombok.Data
    public static class RollbackRequest {
        private String key;
        private Long version;
    }
}
