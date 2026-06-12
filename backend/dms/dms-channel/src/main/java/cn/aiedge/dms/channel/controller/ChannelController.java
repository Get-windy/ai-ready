package cn.aiedge.dms.channel.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.channel.entity.DmsChannel;
import cn.aiedge.dms.channel.service.ChannelService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 配送渠道管理控制器
 */
@Tag(name = "配送渠道管理")
@RestController
@RequestMapping("/api/dms/channel")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @Operation(summary = "分页查询渠道")
    @GetMapping("/page")
    @SaCheckLogin
    public ApiResponse<Page<DmsChannel>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "查询条件") DmsChannel query) {
        Page<DmsChannel> page = new Page<>(pageNum, pageSize);
        return ApiResponse.ok(channelService.page(page, query));
    }

    @Operation(summary = "获取渠道详情")
    @GetMapping("/{id}")
    @SaCheckLogin
    public ApiResponse<DmsChannel> getDetail(@Parameter(description = "渠道ID") @PathVariable Long id) {
        return ApiResponse.ok(channelService.getById(id));
    }

    @Operation(summary = "新增渠道")
    @PostMapping
    @SaCheckPermission("dms:channel:create")
    public ApiResponse<DmsChannel> create(@Parameter(description = "渠道信息") @Valid @RequestBody DmsChannel channel) {
        return ApiResponse.ok("创建成功", channelService.create(channel));
    }

    @Operation(summary = "更新渠道")
    @PutMapping("/{id}")
    @SaCheckPermission("dms:channel:update")
    public ApiResponse<DmsChannel> update(@Parameter(description = "渠道ID") @PathVariable Long id, @Parameter(description = "渠道信息") @Valid @RequestBody DmsChannel channel) {
        return ApiResponse.ok("更新成功", channelService.update(id, channel));
    }

    @Operation(summary = "启停渠道")
    @PutMapping("/{id}/status")
    @SaCheckPermission("dms:channel:update")
    public ApiResponse<Void> updateStatus(@Parameter(description = "渠道ID") @PathVariable Long id, @Parameter(description = "状态: 0=禁用, 1=启用") @RequestParam Integer status) {
        channelService.updateStatus(id, status);
        return ApiResponse.ok(status == 1 ? "已启用" : "已禁用", null);
    }
}
