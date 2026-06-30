package cn.aiedge.trade.controller;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.base.vo.Result;
import cn.aiedge.trade.entity.ExternalChannelConfig;
import cn.aiedge.trade.service.ChannelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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