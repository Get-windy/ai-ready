package cn.aiedge.dms.channel.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.channel.dto.ChannelCallbackRequest;
import cn.aiedge.dms.channel.dto.ChannelPushRequest;
import cn.aiedge.dms.channel.dto.ChannelVO;
import cn.aiedge.dms.channel.entity.DmsChannel;
import cn.aiedge.dms.channel.entity.DmsChannelCallbackLog;
import cn.aiedge.dms.channel.entity.DmsChannelOrder;
import cn.aiedge.dms.channel.service.ChannelOrderService;
import cn.aiedge.dms.channel.service.ChannelService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配送渠道管理控制器
 *
 * 金标准（见《渠道管理开发文档》§3.3）：多条件分页、真删除 + 批量、连通性测试、运力同步。
 * 出参统一为 {@link ChannelVO}（对接凭据脱敏 + 实时运力统计）。
 *
 * @author AI-Ready Team
 */
@Tag(name = "配送渠道管理")
@RestController
@RequestMapping("/api/dms/channel")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;
    private final ChannelOrderService channelOrderService;

    /** 批量操作入参 */
    public record IdsRequest(List<Long> ids) {
    }

    @Operation(summary = "分页查询渠道（多条件）")
    @SaCheckPermission("dms:channel:list")
    @GetMapping("/page")
    @SaCheckLogin
    public ApiResponse<Page<ChannelVO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "渠道编码") @RequestParam(required = false) String channelCode,
            @Parameter(description = "渠道名称") @RequestParam(required = false) String channelName,
            @Parameter(description = "渠道类型") @RequestParam(required = false) Integer channelType,
            @Parameter(description = "对接状态：0-未对接 1-已对接 2-对接异常") @RequestParam(required = false) Integer linkStatus,
            @Parameter(description = "启用状态：0-禁用 1-启用") @RequestParam(required = false) Integer status,
            @Parameter(description = "创建日期起") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "创建日期止") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(channelService.page(pageNum, pageSize, channelCode, channelName,
                channelType, linkStatus, status, startDate, endDate));
    }

    @Operation(summary = "启用渠道下拉（选择器统一口径）")
    @SaCheckPermission("dms:channel:list")
    @GetMapping("/options")
    @SaCheckLogin
    public ApiResponse<List<Map<String, Object>>> options() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (DmsChannel channel : channelService.getAvailableChannels()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", channel.getId());
            item.put("channelCode", channel.getChannelCode());
            item.put("channelName", channel.getChannelName());
            item.put("channelType", channel.getChannelType());
            list.add(item);
        }
        return ApiResponse.ok(list);
    }

    @Operation(summary = "获取渠道详情（对接凭据脱敏）")
    @SaCheckPermission("dms:channel:detail")
    @GetMapping("/{id}")
    @SaCheckLogin
    public ApiResponse<ChannelVO> getDetail(@Parameter(description = "渠道ID") @PathVariable Long id) {
        return ApiResponse.ok(channelService.getDetail(id));
    }

    @Operation(summary = "新增渠道")
    @PostMapping
    @SaCheckPermission("dms:channel:create")
    public ApiResponse<DmsChannel> create(@Parameter(description = "渠道信息") @Valid @RequestBody DmsChannel channel) {
        return ApiResponse.ok("创建成功", channelService.create(channel));
    }

    @Operation(summary = "更新渠道（回传脱敏凭据不会覆盖原值）")
    @PutMapping("/{id}")
    @SaCheckPermission("dms:channel:update")
    public ApiResponse<DmsChannel> update(@Parameter(description = "渠道ID") @PathVariable Long id,
                                          @Parameter(description = "渠道信息") @Valid @RequestBody DmsChannel channel) {
        return ApiResponse.ok("更新成功", channelService.update(id, channel));
    }

    @Operation(summary = "启停渠道")
    @PutMapping("/{id}/status")
    @SaCheckPermission("dms:channel:update")
    public ApiResponse<Void> updateStatus(@Parameter(description = "渠道ID") @PathVariable Long id,
                                          @Parameter(description = "状态: 0=禁用, 1=启用") @RequestParam Integer status) {
        channelService.updateStatus(id, status);
        return ApiResponse.ok(status == 1 ? "已启用" : "已禁用", null);
    }

    @Operation(summary = "批量启停渠道")
    @PostMapping("/batch-status")
    @SaCheckPermission("dms:channel:update")
    public ApiResponse<Integer> batchStatus(@RequestBody(required = false) IdsRequest request,
                                            @Parameter(description = "状态: 0=禁用, 1=启用") @RequestParam Integer status) {
        int affected = channelService.batchStatus(request == null ? null : request.ids(), status);
        return ApiResponse.ok("已处理 " + affected + " 条", affected);
    }

    @Operation(summary = "删除渠道（真删除，被配送员引用时拒绝）")
    @DeleteMapping("/{id}")
    @SaCheckPermission("dms:channel:delete")
    public ApiResponse<Void> delete(@Parameter(description = "渠道ID") @PathVariable Long id) {
        channelService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }

    @Operation(summary = "批量删除渠道（逐条引用保护）")
    @PostMapping("/batch-delete")
    @SaCheckPermission("dms:channel:delete")
    public ApiResponse<Integer> batchDelete(@RequestBody(required = false) IdsRequest request) {
        int affected = channelService.batchDelete(request == null ? null : request.ids());
        return ApiResponse.ok("已删除 " + affected + " 条", affected);
    }

    @Operation(summary = "连通性测试（调适配器探活并回写对接状态）")
    @PostMapping("/{id}/test")
    @SaCheckPermission("dms:channel:update")
    public ApiResponse<ChannelService.TestResult> testConnection(
            @Parameter(description = "渠道ID") @PathVariable Long id) {
        return ApiResponse.ok(channelService.testConnection(id));
    }

    @Operation(summary = "同步外部平台运力（适配器未对接时返回 0 条并说明原因）")
    @PostMapping("/{id}/sync-riders")
    @SaCheckPermission("dms:channel:update")
    public ApiResponse<ChannelService.SyncResult> syncRiders(
            @Parameter(description = "渠道ID") @PathVariable Long id) {
        return ApiResponse.ok(channelService.syncRiders(id));
    }

    // ══════════════════════════════════════════════════════════
    // 派单接线（§3.3 push-order / §7.1）与外部单台账
    // ══════════════════════════════════════════════════════════

    @Operation(summary = "向该渠道下单（渠道派单：幂等 + 失败重试，返回外部单号或失败原因）")
    @PostMapping("/{id}/push-order")
    @SaCheckPermission("dms:channel:update")
    public ApiResponse<ChannelOrderService.PushResult> pushOrder(
            @Parameter(description = "渠道ID") @PathVariable Long id,
            @RequestBody ChannelPushRequest request) {
        if (request == null || request.taskId() == null) {
            return ApiResponse.error("请选择要派单的配送任务");
        }
        // dispatchType=2（手工指派）：页面手选渠道与任务，复盘口径归「手动」
        return ApiResponse.ok(channelOrderService.pushOrder(request.taskId(), id, 2, "渠道管理页手工派单"));
    }

    @Operation(summary = "外部单台账分页（按渠道）")
    @SaCheckPermission("dms:channel:view")
    @GetMapping("/{id}/orders")
    @SaCheckLogin
    public ApiResponse<Page<DmsChannelOrder>> orderPage(
            @Parameter(description = "渠道ID") @PathVariable Long id,
            @Parameter(description = "单状态：0-待提交 1-已提交 2-已接单 3-配送中 4-已完成 5-已取消 6-提交失败")
            @RequestParam(required = false) Integer orderStatus,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.ok(channelOrderService.orderPage(id, orderStatus, pageNum, pageSize));
    }

    @Operation(summary = "回调日志分页（验签/防重放/处理结果留痕）")
    @SaCheckPermission("dms:channel:view")
    @GetMapping("/{id}/callback-logs")
    @SaCheckLogin
    public ApiResponse<Page<DmsChannelCallbackLog>> callbackLogPage(
            @Parameter(description = "渠道ID") @PathVariable Long id,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.ok(channelOrderService.callbackLogPage(id, pageNum, pageSize));
    }

    /**
     * 外部平台回调（**免登录**，见 SaTokenConfig 放行；安全由验签 + 时间戳容差 + nonce 防重放保证）。
     *
     * <p>签名口径：{@code HEX(HMAC-SHA256(appSecret 或 signKey,
     * channelCode + timestamp + nonce + channelOrderNo + status))}。</p>
     */
    @Operation(summary = "外部平台回调（验签 + 防重放 + 状态回写）")
    @PostMapping("/callback")
    @SaIgnore
    public ApiResponse<ChannelOrderService.CallbackResult> callback(@RequestBody ChannelCallbackRequest request) {
        return ApiResponse.ok(channelOrderService.handleCallback(request));
    }
}
