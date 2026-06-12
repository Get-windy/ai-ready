package cn.aiedge.dms.config.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.common.constant.DmsConstants;
import cn.aiedge.dms.config.entity.DmsConfig;
import cn.aiedge.dms.config.service.ConfigService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    @GetMapping("/{key}")
    public ApiResponse<DmsConfig> getConfig(
            @Parameter(description = "租户ID") @RequestParam(defaultValue = "0") Long tenantId,
            @Parameter(description = "配置键") @PathVariable String key) {
        return ApiResponse.success(configService.getConfig(tenantId, key));
    }

    @Operation(summary = "更新指定配置")
    @PutMapping("/{key}")
    @SaCheckPermission("dms:config:update")
    public ApiResponse<DmsConfig> updateConfig(
            @Parameter(description = "租户ID") @RequestParam(defaultValue = "0") Long tenantId,
            @Parameter(description = "配置键") @PathVariable String key,
            @Parameter(description = "配置值") @RequestBody String value) {
        return ApiResponse.success(configService.updateConfig(tenantId, key, value));
    }

    @Operation(summary = "获取租户所有配置")
    @GetMapping("/list")
    public ApiResponse<List<DmsConfig>> listAll(
            @Parameter(description = "租户ID") @RequestParam(defaultValue = "0") Long tenantId) {
        return ApiResponse.success(configService.listAll(tenantId));
    }
}
