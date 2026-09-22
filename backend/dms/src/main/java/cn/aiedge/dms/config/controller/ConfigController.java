package cn.aiedge.dms.config.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.config.dto.ConfigExportVO;
import cn.aiedge.dms.config.dto.ConfigImportRequest;
import cn.aiedge.dms.config.dto.ConfigImportResultVO;
import cn.aiedge.dms.config.dto.ConfigItemDTO;
import cn.aiedge.dms.config.dto.ConfigItemVO;
import cn.aiedge.dms.config.dto.ConfigQueryDTO;
import cn.aiedge.dms.common.constant.DmsConstants;
import cn.aiedge.dms.config.entity.DmsConfig;
import cn.aiedge.dms.config.entity.DmsConfigHistory;
import cn.aiedge.dms.config.service.ConfigService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 租户配置管理控制器
 */
@Tag(name = "租户配置管理")
@RestController
@RequestMapping("/api/dms/config")
@RequiredArgsConstructor
@SaCheckLogin
public class ConfigController {

    private final ConfigService configService;

    @Operation(summary = "获取指定配置")
    @SaCheckPermission("dms:config:detail")
    @GetMapping("/{key}")
    public ApiResponse<DmsConfig> getConfig(
            @Parameter(description = "租户ID") @RequestParam(required = false) Long tenantId,
            @Parameter(description = "配置键") @PathVariable String key) {
        return ApiResponse.success(configService.getConfig(tenantId, key));
    }

    @Operation(summary = "更新指定配置")
    @PutMapping("/{key}")
    @SaCheckPermission("dms:config:update")
    public ApiResponse<DmsConfig> updateConfig(
            @Parameter(description = "租户ID") @RequestParam(required = false) Long tenantId,
            @Parameter(description = "配置键") @PathVariable String key,
            @Parameter(description = "配置值") @RequestBody String value) {
        return ApiResponse.success(configService.updateConfig(tenantId, key, normalize(value)));
    }

    /**
     * 归一化配置值
     *
     * 前端《配送参数》以 `JSON.stringify(值)` 作为请求体（axios 直接把字符串当 body 发），
     * Spring 的 String 转换器会连同引号一起返回（如 `"30"`），导致：
     * ① 数值型配置 getInteger 解析失败；② 清空配置时存成 `""` 而非空串（Key 无法撤销）。
     * 这里剥掉一层 JSON 字符串引号，使「页面保存 = 存裸值」。
     */
    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return value;
    }

    @Operation(summary = "清空指定配置值（敏感键也支持，等价于撤销配置）")
    @DeleteMapping("/{key}")
    @SaCheckPermission("dms:config:update")
    public ApiResponse<DmsConfig> clearConfig(
            @Parameter(description = "租户ID") @RequestParam(required = false) Long tenantId,
            @Parameter(description = "配置键") @PathVariable String key) {
        return ApiResponse.success(configService.clearConfig(tenantId, key));
    }

    @Operation(summary = "配置变更历史（敏感键只返回掩码）")
    @SaCheckPermission("dms:config:view")
    @GetMapping("/{key}/history")
    public ApiResponse<List<DmsConfigHistory>> history(
            @Parameter(description = "租户ID") @RequestParam(required = false) Long tenantId,
            @Parameter(description = "配置键") @PathVariable String key) {
        return ApiResponse.success(configService.history(tenantId, key));
    }

    @Operation(summary = "回滚配置到指定历史版本（按历史的「变更前值」写回）")
    @PostMapping("/{key}/rollback")
    @SaCheckPermission("dms:config:update")
    public ApiResponse<DmsConfig> rollback(
            @Parameter(description = "租户ID") @RequestParam(required = false) Long tenantId,
            @Parameter(description = "配置键") @PathVariable String key,
            @Parameter(description = "历史记录ID") @RequestParam Long historyId) {
        return ApiResponse.success(configService.rollback(tenantId, key, historyId));
    }

    @Operation(summary = "获取租户所有配置")
    @SaCheckPermission("dms:config:list")
    @GetMapping("/list")
    public ApiResponse<List<DmsConfig>> listAll(
            @Parameter(description = "租户ID") @RequestParam(required = false) Long tenantId) {
        return ApiResponse.success(configService.listAll(tenantId));
    }

    // ==================== 参数中心（金标准：元数据驱动 / 分页 / 批量保存 / 恢复默认） ====================

    @Operation(summary = "参数中心分页（多条件；行内含元数据：类型/默认值/范围/单位/生效方式）")
    @SaCheckPermission("dms:config:list")
    @GetMapping("/page")
    public ApiResponse<Page<ConfigItemVO>> page(ConfigQueryDTO query) {
        return ApiResponse.success(configService.page(query));
    }

    @Operation(summary = "参数元数据 + 分组树计数（驱动前端按类型渲染控件）")
    @SaCheckPermission("dms:config:view")
    @GetMapping("/meta")
    public ApiResponse<Map<String, Object>> meta() {
        return ApiResponse.success(configService.meta());
    }

    @Operation(summary = "批量保存参数（事务；逐项类型/范围/枚举/JSON/时间范围校验；敏感键留空=不修改）")
    @SaCheckPermission("dms:config:update")
    @PutMapping("/batch")
    public ApiResponse<Integer> batchUpdate(@RequestBody List<ConfigItemDTO> items) {
        return ApiResponse.success("保存成功", configService.batchUpdate(items));
    }

    @Operation(summary = "恢复默认值（依据元数据注册表）")
    @SaCheckPermission("dms:config:update")
    @PostMapping("/{key}/reset")
    public ApiResponse<DmsConfig> reset(
            @Parameter(description = "租户ID") @RequestParam(required = false) Long tenantId,
            @Parameter(description = "配置键") @PathVariable String key) {
        return ApiResponse.success("已恢复默认", configService.reset(tenantId, key));
    }

    @Operation(summary = "导出参数集（JSON 参数清单；按查询条件过滤；敏感键不导出明文）")
    @SaCheckPermission("dms:config:export")
    @GetMapping("/export")
    public ApiResponse<ConfigExportVO> exportConfigs(ConfigQueryDTO query) {
        return ApiResponse.success(configService.exportConfigs(query));
    }

    @Operation(summary = "导入参数集（PREVIEW 预览差异 / APPLY 落库；逐项校验，存在失败项整体拒绝）")
    @PostMapping("/import")
    @SaCheckPermission("dms:config:update")
    public ApiResponse<ConfigImportResultVO> importConfigs(@RequestBody ConfigImportRequest request) {
        return ApiResponse.success(configService.importConfigs(request));
    }
}
