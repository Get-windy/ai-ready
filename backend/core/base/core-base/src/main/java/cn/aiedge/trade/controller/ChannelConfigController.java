package cn.aiedge.trade.controller;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.base.vo.Result;
import cn.aiedge.trade.entity.ExternalChannelConfig;
import cn.aiedge.trade.service.ChannelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 渠道配置管理控制器
 */
@Tag(name = "渠道配置管理", description = "外部平台渠道配置")
@RestController
@RequestMapping("/api/trade/channel")
@RequiredArgsConstructor
@Validated
public class ChannelConfigController {

    private final ChannelConfigService service;

    @Operation(summary = "创建渠道配置")
    @PostMapping
    public Result<ExternalChannelConfig> create(@RequestBody ExternalChannelConfig config) {
        return Result.success(service.create(config));
    }

    @Operation(summary = "更新渠道配置")
    @PutMapping("/{id}")
    public Result<ExternalChannelConfig> update(@PathVariable Long id, @RequestBody ExternalChannelConfig config) {
        return Result.success(service.update(id, config));
    }

    @Operation(summary = "删除渠道配置")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success();
    }

    @Operation(summary = "查询渠道配置")
    @GetMapping("/{id}")
    public Result<ExternalChannelConfig> get(@PathVariable Long id) {
        return Result.success(service.get(id));
    }

    @Operation(summary = "按编码查询渠道")
    @GetMapping("/code/{channelCode}")
    public Result<ExternalChannelConfig> getByCode(@PathVariable String channelCode) {
        return Result.success(service.getByCode(channelCode));
    }

    @Operation(summary = "查询所有启用的渠道")
    @GetMapping("/enabled")
    public Result<List<ExternalChannelConfig>> listEnabled() {
        return Result.success(service.listEnabled());
    }

    @Operation(summary = "渠道配置分页查询（管理端列表）")
    @GetMapping("/page")
    public Result<PageResult<ExternalChannelConfig>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "关键字（渠道编码/渠道名称）") @RequestParam(required = false) String keyword,
            @Parameter(description = "渠道类型 ECOMMERCE/SOCIAL/SELF/ERP") @RequestParam(required = false) String channelType,
            @Parameter(description = "同步开关 1开/0关") @RequestParam(required = false) Integer syncEnabled,
            @Parameter(description = "启用状态 1正常/0禁用") @RequestParam(required = false) Integer status) {
        return Result.success(service.pageChannels(pageNum, pageSize, keyword, channelType, syncEnabled, status));
    }

    @Operation(summary = "渠道台账统计",
            description = "真实聚合 SQL：渠道总数 / 启用数 / 同步开启数 / 异常数（禁用或令牌已过期），非当前页口径")
    @GetMapping("/stat")
    public Result<Map<String, Object>> stat() {
        return Result.success(service.statChannels());
    }

    @Operation(summary = "查看渠道密钥（脱敏）",
            description = "appSecret / accessToken / refreshToken 一律脱敏返回，不返回明文；未配置的项 configured=false")
    @GetMapping("/{id}/secret")
    public Result<Map<String, Object>> secret(@PathVariable Long id) {
        ExternalChannelConfig config = service.get(id);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id);
        if (config == null) {
            return Result.success(data);
        }
        data.put("channelCode", config.getChannelCode());
        data.put("channelName", config.getChannelName());
        data.put("appId", config.getAppId());
        data.put("appSecret", mask(config.getAppSecret()));
        data.put("appSecretConfigured", StringUtils.hasText(config.getAppSecret()));
        data.put("accessToken", mask(config.getAccessToken()));
        data.put("accessTokenConfigured", StringUtils.hasText(config.getAccessToken()));
        data.put("refreshToken", mask(config.getRefreshToken()));
        data.put("refreshTokenConfigured", StringUtils.hasText(config.getRefreshToken()));
        data.put("tokenExpireTime", config.getTokenExpireTime());
        return Result.success(data);
    }

    /** 密钥脱敏：保留前 2 位 + 后 2 位，中间以 * 填充（长度不足 6 位时全掩码） */
    private static String mask(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String value = raw.trim();
        if (value.length() <= 6) {
            return "******";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    @Operation(summary = "启用/禁用渠道")
    @PostMapping("/{id}/toggle")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam boolean enabled) {
        service.toggleStatus(id, enabled);
        return Result.success();
    }

    @Operation(summary = "初始化渠道连接")
    @PostMapping("/{id}/initialize")
    public Result<Boolean> initialize(@PathVariable Long id) {
        return Result.success(service.initializeChannel(id));
    }

    @Operation(summary = "同步渠道数据", description = "从外部平台同步订单/商品等数据")
    @PostMapping("/{id}/sync")
    public Result<java.util.Map<String, Object>> sync(@PathVariable Long id) {
        java.util.Map<String, Object> result = service.syncChannelData(id);
        return Result.success(result);
    }
}