package cn.aiedge.payment.controller;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.base.vo.Result;
import cn.aiedge.payment.entity.PaymentReconciliation;
import cn.aiedge.payment.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "支付对账", description = "日对账、差异处理")
@RestController
@RequestMapping("/api/reconciliation")
@RequiredArgsConstructor
@Validated
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @Operation(summary = "执行日对账")
    @PostMapping("/execute")
    public Result<List<PaymentReconciliation>> executeDailyReconciliation(
            @Parameter(description = "对账日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @Parameter(description = "渠道(可选)") @RequestParam(required = false) String channel) {
        return Result.success(reconciliationService.executeDailyReconciliation(date, channel));
    }

    @Operation(summary = "分页查询对账记录")
    @GetMapping("/page")
    public Result<PageResult<PaymentReconciliation>> pageReconciliation(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "渠道") @RequestParam(required = false) String channel,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        return Result.success(reconciliationService.pageReconciliation(pageNum, pageSize, startDate, endDate, channel, status));
    }

    @Operation(summary = "查询对账详情")
    @GetMapping("/{id}")
    public Result<PaymentReconciliation> getReconciliation(@PathVariable Long id) {
        return Result.success(reconciliationService.getReconciliation(id));
    }

    @Operation(summary = "处理差异")
    @PostMapping("/{id}/handle")
    public Result<Void> handleDifference(
            @PathVariable Long id,
            @RequestParam String remark) {
        reconciliationService.handleDifference(id, remark);
        return Result.success();
    }

    @Operation(summary = "获取待对账日期列表")
    @GetMapping("/pending-dates")
    public Result<List<LocalDate>> getPendingDates(
            @Parameter(description = "渠道") @RequestParam(required = false) String channel) {
        return Result.success(reconciliationService.getPendingDates(channel));
    }
}