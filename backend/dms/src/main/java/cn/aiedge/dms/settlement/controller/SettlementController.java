package cn.aiedge.dms.settlement.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.settlement.service.SettlementService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 费用结算控制器
 *
 * 提供配送费计算、结算报表查询、ERP推送等REST接口。
 *
 * @author AI-Ready Team
 */
@Tag(name = "费用结算")
@RestController
@RequestMapping("/api/dms/settlement")
@RequiredArgsConstructor
@SaCheckLogin
public class SettlementController {

    private final SettlementService settlementService;

    @Operation(summary = "计算配送费")
    @GetMapping("/fee/{taskId}")
    public ApiResponse<Map<String, Object>> calculateFee(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        Map<String, Object> fee = settlementService.calculateDeliveryFee(taskId);
        return ApiResponse.success(fee);
    }

    @Operation(summary = "生成结算报表")
    @GetMapping("/report")
    public ApiResponse<Map<String, Object>> generateReport(
            @Parameter(description = "租户ID") @RequestParam Long tenantId,
            @Parameter(description = "起始日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Map<String, Object> report = settlementService.generateSettlementReport(tenantId, startDate, endDate);
        return ApiResponse.success(report);
    }

    @Operation(summary = "推送结算数据到ERP")
    @PostMapping("/push-erp")
    @SaCheckPermission("dms:settlement:push")
    public ApiResponse<Void> pushToErp(
            @RequestBody Map<String, Object> settlementData) {
        settlementService.pushToErp(settlementData);
        return ApiResponse.success();
    }
}
