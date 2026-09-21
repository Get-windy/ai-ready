package cn.aiedge.integration.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.integration.model.SyncDataSourceConfig;
import cn.aiedge.integration.service.SyncConfigService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 同步数据源配置控制器
 *
 * 租户管理员通过此接口管理外部系统绑定和同步设置。
 *
 * <p><b>鉴权（2026-09-21 补，E-02 批次 5 —— 本类归"没接线"而非"残留"）：</b>
 * 本类原先只有类级 {@code @SaCheckLogin}，九个端点对任何登录用户开放；
 * 而库里 {@code system:dataimport:*} 六个动作码**早就造好了却没挂上去** ——
 * 且 {@code sys_permission.api_path} 填的正是 {@code /api/v1/sync-config}
 * （{@code V11.450.0} 回填时从本控制器取的），即"码在等接口"。
 * 现在两侧对齐，前端 {@code views/system/data-import/index.vue} 调的就是这些端点。</p>
 */
@RestController
@RequestMapping("/api/v1/sync-config")
@RequiredArgsConstructor
@SaCheckLogin
@Tag(name = "数据导入配置", description = "租户绑定外部系统、设置同步参数")
public class SyncConfigController {

    private final SyncConfigService syncConfigService;

    // ==================== 获取来源系统列表（下拉选择） ====================

    @GetMapping("/sources")
    @Operation(summary = "获取支持的导入系统列表（下拉选择用）")
    @SaCheckPermission("system:dataimport:list")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getSupportedSources() {
        return ResponseEntity.ok(ApiResponse.success(syncConfigService.getSupportedSources()));
    }

    // ==================== CRUD ====================

    @PostMapping
    @Operation(summary = "创建同步数据源配置")
    @SaCheckPermission("system:dataimport:create")
    public ResponseEntity<ApiResponse<SyncDataSourceConfig>> createConfig(@RequestBody SyncDataSourceConfig config) {
        SyncDataSourceConfig saved = syncConfigService.createConfig(config);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新同步数据源配置")
    @SaCheckPermission("system:dataimport:update")
    public ResponseEntity<ApiResponse<SyncDataSourceConfig>> updateConfig(
            @PathVariable Long id,
            @RequestBody SyncDataSourceConfig config) {
        SyncDataSourceConfig updated = syncConfigService.updateConfig(id, config);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除同步数据源配置")
    @SaCheckPermission("system:dataimport:delete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteConfig(@PathVariable Long id) {
        boolean deleted = syncConfigService.deleteConfig(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("success", deleted)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取同步数据源配置详情")
    @SaCheckPermission("system:dataimport:list")
    public ResponseEntity<ApiResponse<SyncDataSourceConfig>> getConfig(@PathVariable Long id) {
        SyncDataSourceConfig config = syncConfigService.getConfig(id);
        if (config == null) {
            return ResponseEntity.ok(ApiResponse.success((SyncDataSourceConfig) null));
        }
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @GetMapping
    @Operation(summary = "获取当前租户的所有同步配置")
    @SaCheckPermission("system:dataimport:list")
    public ResponseEntity<ApiResponse<List<SyncDataSourceConfig>>> listConfigs() {
        return ResponseEntity.ok(ApiResponse.success(syncConfigService.listConfigs()));
    }

    @PostMapping("/{id}/toggle")
    @Operation(summary = "启用/禁用同步配置")
    @SaCheckPermission("system:dataimport:update")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleConfig(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        boolean success = syncConfigService.toggleStatus(id, enabled);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "success", success,
                "status", enabled ? "enabled" : "disabled"
        )));
    }

    // ==================== 操作 ====================

    @PostMapping("/{id}/test")
    @Operation(summary = "测试与外部系统的连接")
    @SaCheckPermission("system:dataimport:test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testConnection(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(syncConfigService.testConnection(id)));
    }

    @PostMapping("/{id}/sync")
    @Operation(summary = "手动触发同步")
    @SaCheckPermission("system:dataimport:sync")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerSync(
            @PathVariable Long id,
            @RequestParam(defaultValue = "incremental") String syncType) {
        return ResponseEntity.ok(ApiResponse.success(syncConfigService.triggerSync(id, syncType)));
    }
}
