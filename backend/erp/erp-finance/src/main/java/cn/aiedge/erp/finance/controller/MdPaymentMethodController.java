package cn.aiedge.erp.finance.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.model.entity.PaymentMethod;
import cn.aiedge.erp.finance.service.PaymentMethodService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 支付方式主数据Controller
 * 对应表 md_payment_method（V11.29.0 新增）
 */
@Tag(name = "支付方式管理")
@RestController
@RequestMapping("/api/erp/md/payment-method")
@RequiredArgsConstructor
public class MdPaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @Operation(summary = "分页查询支付方式")
    @OperationLog(module = "支付方式管理", type = "QUERY", desc = "分页查询支付方式")
    @PreAuthorize("hasPermission('/api/erp/md/payment-method/page', 'md:payment-method:view')")
    @GetMapping("/page")
    public Result<Page<PaymentMethod>> page(
            @Parameter(description = "关键词（编码/名称模糊）") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        LambdaQueryWrapper<PaymentMethod> wrapper = new LambdaQueryWrapper<PaymentMethod>()
                .and(keyword != null, w -> w.like(PaymentMethod::getMethodCode, keyword)
                        .or().like(PaymentMethod::getMethodName, keyword))
                .eq(status != null, PaymentMethod::getStatus, status)
                .orderByAsc(PaymentMethod::getSort)
                .orderByDesc(PaymentMethod::getCreateTime);
        return Result.success(paymentMethodService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    @Operation(summary = "查询支付方式详情")
    @OperationLog(module = "支付方式管理", type = "QUERY", desc = "查询支付方式详情")
    @PreAuthorize("hasPermission('/api/erp/md/payment-method/detail', 'md:payment-method:view')")
    @GetMapping("/{id}")
    public Result<PaymentMethod> getById(@PathVariable Long id) {
        return Result.success(paymentMethodService.getById(id));
    }

    @Operation(summary = "查询所有启用的支付方式（下拉选择）")
    @OperationLog(module = "支付方式管理", type = "QUERY", desc = "查询下拉列表")
    @GetMapping("/list")
    public Result<java.util.List<PaymentMethod>> list() {
        LambdaQueryWrapper<PaymentMethod> wrapper = new LambdaQueryWrapper<PaymentMethod>()
                .eq(PaymentMethod::getStatus, 1)
                .orderByAsc(PaymentMethod::getSort);
        return Result.success(paymentMethodService.list(wrapper));
    }

    @Operation(summary = "新增支付方式")
    @OperationLog(module = "支付方式管理", type = "CREATE", desc = "新增支付方式")
    @PreAuthorize("hasPermission('/api/erp/md/payment-method/create', 'md:payment-method:edit')")
    @PostMapping
    public Result<Boolean> create(@RequestBody PaymentMethod entity) {
        return Result.success(paymentMethodService.save(entity));
    }

    @Operation(summary = "更新支付方式")
    @OperationLog(module = "支付方式管理", type = "UPDATE", desc = "更新支付方式")
    @PreAuthorize("hasPermission('/api/erp/md/payment-method/update', 'md:payment-method:edit')")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody PaymentMethod entity) {
        entity.setId(id);
        return Result.success(paymentMethodService.updateById(entity));
    }

    @Operation(summary = "删除支付方式（引用保护）")
    @OperationLog(module = "支付方式管理", type = "DELETE", desc = "删除支付方式")
    @PreAuthorize("hasPermission('/api/erp/md/payment-method/delete', 'md:payment-method:edit')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // TODO: 检查是否被支付渠道或收付款单引用，被引用时禁止删除
        paymentMethodService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "启用/停用支付方式")
    @OperationLog(module = "支付方式管理", type = "UPDATE", desc = "更新支付方式状态")
    @PreAuthorize("hasPermission('/api/erp/md/payment-method/status', 'md:payment-method:edit')")
    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id, @RequestBody Integer status) {
        return Result.success(paymentMethodService.updateStatus(id, status));
    }
}
