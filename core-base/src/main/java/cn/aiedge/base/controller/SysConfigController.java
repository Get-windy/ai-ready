package cn.aiedge.base.controller;

import cn.aiedge.base.entity.SysProjectConfig;
import cn.aiedge.base.service.SysConfigService;
import cn.aiedge.base.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统配置管理控制器
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
    public Result<String> getValue(@PathVariable String key) {
        return Result.success(configService.getValue(key, null));
    }

    @GetMapping("/get/{key}")
    @Operation(summary = "获取配置对象")
    public Result<SysProjectConfig> getConfig(@PathVariable String key) {
        return Result.success(configService.getConfig(key).orElse(null));
    }

    @GetMapping("/group/{group}")
    @Operation(summary = "获取配置分组")
    public Result<List<SysProjectConfig>> getConfigsByGroup(@PathVariable String group) {
        return Result.success(configService.getConfigsByGroup(group));
    }

    @GetMapping("/list")
    @Operation(summary = "获取所有配置")
    public Result<List<SysProjectConfig>> getAllConfigs() {
        return Result.success(configService.getAllConfigs());
    }

    @GetMapping("/map")
    @Operation(summary = "获取配置Map")
    public Result<Map<String, String>> getConfigMap() {
        return Result.success(configService.getConfigMap());
    }

    // ==================== 配置更新 ====================

    @PostMapping("/set")
    @Operation(summary = "设置配置")
    public Result<Void> setValue(@RequestBody ConfigRequest request) {
        configService.setValue(
            request.getKey(), 
            request.getValue(),
            request.getType(),
            request.getGroup(),
            request.getDescription()
        );
        return Result.success();
    }

    @PostMapping("/batch")
    @Operation(summary = "批量设置配置")
    public Result<Void> setValues(@RequestBody Map<String, String> configs) {
        configService.setValues(configs);
        return Result.success();
    }

    @DeleteMapping("/{key}")
    @Operation(summary = "删除配置")
    public Result<Void> deleteConfig(@PathVariable String key) {
        configService.deleteConfig(key);
        return Result.success();
    }

    // ==================== 配置热更新 ====================

    @PostMapping("/refresh")
    @Operation(summary = "刷新所有配置缓存")
    public Result<Void> refreshCache() {
        configService.refreshCache();
        return Result.success();
    }

    @PostMapping("/refresh/{key}")
    @Operation(summary = "刷新指定配置")
    public Result<Void> refreshConfig(@PathVariable String key) {
        configService.refreshConfig(key);
        return Result.success();
    }

    // ==================== 配置版本管理 ====================

    @GetMapping("/history/{key}")
    @Operation(summary = "获取配置历史")
    public Result<List<SysConfigService.ConfigHistory>> getConfigHistory(@PathVariable String key) {
        return Result.success(configService.getConfigHistory(key));
    }

    @PostMapping("/rollback")
    @Operation(summary = "回滚配置")
    public Result<Void> rollbackConfig(@RequestBody RollbackRequest request) {
        configService.rollbackConfig(request.getKey(), request.getVersion());
        return Result.success();
    }

    @GetMapping("/compare")
    @Operation(summary = "比较配置版本")
    public Result<SysConfigService.ConfigDiff> compareVersions(
            @RequestParam String key,
            @RequestParam Long version1,
            @RequestParam Long version2) {
        return Result.success(configService.compareVersions(key, version1, version2));
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
