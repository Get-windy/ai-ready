package cn.aiedge.erp.finance.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.model.entity.PaymentChannel;
import cn.aiedge.erp.finance.service.PaymentChannelService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 支付渠道主数据Controller
 * 对应表 md_payment_channel（V11.29.0 新增）
 */
@Tag(name = "支付渠道管理")
@RestController
@RequestMapping("/api/erp/md/payment-channel")
@RequiredArgsConstructor
public class MdPaymentChannelController {

    private final PaymentChannelService paymentChannelService;

    @Operation(summary = "分页查询支付渠道")
    @OperationLog(module = "支付渠道管理", type = "QUERY", desc = "分页查询支付渠道")
    @PreAuthorize("hasPermission('/api/erp/md/payment-channel/page', 'md:payment-channel:view')")
    @GetMapping("/page")
    public Result<Page<PaymentChannel>> page(
            @Parameter(description = "关键词（编码/名称模糊）") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "支付方式ID") @RequestParam(required = false) Long methodId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        LambdaQueryWrapper<PaymentChannel> wrapper = new LambdaQueryWrapper<PaymentChannel>()
                .and(keyword != null, w -> w.like(PaymentChannel::getChannelCode, keyword)
                        .or().like(PaymentChannel::getChannelName, keyword))
                .eq(status != null, PaymentChannel::getStatus, status)
                .eq(methodId != null, PaymentChannel::getMethodId, methodId)
                .orderByAsc(PaymentChannel::getSort)
                .orderByDesc(PaymentChannel::getCreateTime);
        return Result.success(paymentChannelService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    @Operation(summary = "查询支付渠道详情")
    @OperationLog(module = "支付渠道管理", type = "QUERY", desc = "查询支付渠道详情")
    @PreAuthorize("hasPermission('/api/erp/md/payment-channel/detail', 'md:payment-channel:view')")
    @GetMapping("/{id}")
    public Result<PaymentChannel> getById(@PathVariable Long id) {
        return Result.success(paymentChannelService.getById(id));
    }

    @Operation(summary = "查询所有启用的支付渠道（下拉选择）")
    @GetMapping("/list")
    public Result<java.util.List<PaymentChannel>> list(
            @Parameter(description = "支付方式ID筛选") @RequestParam(required = false) Long methodId) {
        LambdaQueryWrapper<PaymentChannel> wrapper = new LambdaQueryWrapper<PaymentChannel>()
                .eq(PaymentChannel::getStatus, 1)
                .eq(methodId != null, PaymentChannel::getMethodId, methodId)
                .orderByAsc(PaymentChannel::getSort);
        return Result.success(paymentChannelService.list(wrapper));
    }

    @Operation(summary = "新增支付渠道")
    @OperationLog(module = "支付渠道管理", type = "CREATE", desc = "新增支付渠道")
    @PreAuthorize("hasPermission('/api/erp/md/payment-channel/create', 'md:payment-channel:edit')")
    @PostMapping
    public Result<Boolean> create(@RequestBody PaymentChannel entity) {
        return Result.success(paymentChannelService.save(entity));
    }

    @Operation(summary = "更新支付渠道")
    @OperationLog(module = "支付渠道管理", type = "UPDATE", desc = "更新支付渠道")
    @PreAuthorize("hasPermission('/api/erp/md/payment-channel/update', 'md:payment-channel:edit')")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody PaymentChannel entity) {
        entity.setId(id);
        return Result.success(paymentChannelService.updateById(entity));
    }

    @Operation(summary = "删除支付渠道（引用保护）")
    @OperationLog(module = "支付渠道管理", type = "DELETE", desc = "删除支付渠道")
    @PreAuthorize("hasPermission('/api/erp/md/payment-channel/delete', 'md:payment-channel:edit')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // TODO: 检查是否被收付款单引用，被引用时禁止删除
        paymentChannelService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "启用/停用支付渠道")
    @OperationLog(module = "支付渠道管理", type = "UPDATE", desc = "更新支付渠道状态")
    @PreAuthorize("hasPermission('/api/erp/md/payment-channel/status', 'md:payment-channel:edit')")
    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id, @RequestBody Integer status) {
        return Result.success(paymentChannelService.updateStatus(id, status));
    }
}
