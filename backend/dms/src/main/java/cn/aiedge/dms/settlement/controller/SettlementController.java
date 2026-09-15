package cn.aiedge.dms.settlement.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.settlement.entity.DmsSettlement;
import cn.aiedge.dms.settlement.entity.DmsSettlementItem;
import cn.aiedge.dms.settlement.entity.DmsSettlementRule;
import cn.aiedge.dms.settlement.service.SettlementRuleService;
import cn.aiedge.dms.settlement.service.SettlementService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 配送结算控制器（配送 → 结算收款 → 配送结算，菜单 80910）
 *
 * <p>三块能力：</p>
 * <ol>
 *   <li><b>计费规则</b>：规则表 CRUD（按 结算对象 × 渠道/线路 × 生效期 × 优先级），未命中回落全局缺省费率；</li>
 *   <li><b>结算单闭环</b>：生成（草稿）→ 确认（锁定金额）→ 推送 ERP（**真实记账**：凭证 + 外部运力应付，幂等）；</li>
 *   <li><b>对账</b>：结算单 vs 财务（凭证金额 / 应付核销 / 配送费收款），输出差异清单。</li>
 * </ol>
 */
@Tag(name = "配送结算")
@RestController
@RequestMapping("/api/dms/settlement")
@RequiredArgsConstructor
@SaCheckLogin
public class SettlementController {

    private final SettlementService settlementService;
    private final SettlementRuleService ruleService;

    // ═══ 计费规则 ═══

    @Operation(summary = "全局缺省计费费率（配送参数 dms.settlement.*）")
    @GetMapping("/rule")
    public ApiResponse<Map<String, BigDecimal>> rule() {
        return ApiResponse.ok(settlementService.currentRule());
    }

    @Operation(summary = "计费规则分页")
    @GetMapping("/rule/page")
    public ApiResponse<IPage<DmsSettlementRule>> rulePage(
            @RequestParam(required = false) String ruleCode,
            @RequestParam(required = false) String ruleName,
            @RequestParam(required = false) Integer targetType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(ruleService.page(ruleCode, ruleName, targetType, status, current, size));
    }

    @Operation(summary = "启用中的计费规则（供生成结算单预览）")
    @GetMapping("/rule/enabled")
    public ApiResponse<List<DmsSettlementRule>> ruleEnabled(@RequestParam(required = false) Integer targetType) {
        return ApiResponse.ok(ruleService.enabledRules(targetType));
    }

    @Operation(summary = "新增计费规则")
    @PostMapping("/rule")
    public ApiResponse<DmsSettlementRule> ruleCreate(@RequestBody DmsSettlementRule rule) {
        return ApiResponse.ok(ruleService.create(rule));
    }

    @Operation(summary = "修改计费规则")
    @PutMapping("/rule/{id}")
    public ApiResponse<DmsSettlementRule> ruleUpdate(@PathVariable Long id, @RequestBody DmsSettlementRule rule) {
        return ApiResponse.ok(ruleService.update(id, rule));
    }

    @Operation(summary = "启停计费规则")
    @PostMapping("/rule/{id}/status")
    public ApiResponse<DmsSettlementRule> ruleStatus(@PathVariable Long id,
                                                     @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(ruleService.updateStatus(id, status));
    }

    @Operation(summary = "删除计费规则")
    @DeleteMapping("/rule/{id}")
    public ApiResponse<Void> ruleDelete(@PathVariable Long id) {
        ruleService.delete(id);
        return ApiResponse.ok(null);
    }

    // ═══ 算费 / 报表 ═══

    @Operation(summary = "计算单任务配送费（含命中的计费规则与签收折算）")
    @GetMapping("/fee/{taskId}")
    public ApiResponse<Map<String, Object>> calculateFee(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        return ApiResponse.ok(settlementService.calculateDeliveryFee(taskId));
    }

    @Operation(summary = "周期结算报表（按配送员/按日聚合，口径=已签收/已完成）")
    @GetMapping("/report")
    public ApiResponse<Map<String, Object>> generateReport(
            @Parameter(description = "租户ID") @RequestParam(required = false) Long tenantId,
            @Parameter(description = "起始日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(settlementService.generateSettlementReport(tenantId, startDate, endDate));
    }

    // ═══ 结算单 ═══

    @Operation(summary = "结算单分页")
    @GetMapping("/page")
    public ApiResponse<IPage<DmsSettlement>> page(
            @RequestParam(required = false) String settlementNo,
            @RequestParam(required = false) Integer targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(settlementService.page(settlementNo, targetType, targetId, status,
                startDate, endDate, current, size));
    }

    @Operation(summary = "生成结算单（按周期+结算对象聚合已签收任务；对象=配送员/渠道）")
    @PostMapping("/generate")
    public ApiResponse<DmsSettlement> generate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
            @RequestParam(required = false, defaultValue = "1") Integer targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) String remark) {
        return ApiResponse.ok(settlementService.generate(periodStart, periodEnd, targetType, targetId, remark));
    }

    @Operation(summary = "结算单详情")
    @GetMapping("/{id}")
    public ApiResponse<DmsSettlement> detail(@PathVariable Long id) {
        return ApiResponse.ok(settlementService.getById(id));
    }

    @Operation(summary = "结算单明细（费用构成 + 签收口径）")
    @GetMapping("/{id}/items")
    public ApiResponse<List<DmsSettlementItem>> items(@PathVariable Long id) {
        return ApiResponse.ok(settlementService.items(id));
    }

    @Operation(summary = "确认结算单（锁定金额）")
    @PostMapping("/{id}/confirm")
    public ApiResponse<DmsSettlement> confirm(@PathVariable Long id,
                                              @RequestParam(required = false) String remark) {
        return ApiResponse.ok(settlementService.confirm(id, remark));
    }

    @Operation(summary = "推送结算单到 ERP（真实记账：凭证+应付；幂等：已推送返回既有结果）")
    @PostMapping("/{id}/push-erp")
    public ApiResponse<Map<String, Object>> pushErp(@PathVariable Long id) {
        return ApiResponse.ok(settlementService.pushErp(id));
    }

    @Operation(summary = "删除结算单（仅草稿）")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        settlementService.delete(id);
        return ApiResponse.ok(null);
    }

    // ═══ 对账 ═══

    @Operation(summary = "结算对账（凭证金额 / 应付核销 / 配送费收款，输出差异清单）")
    @GetMapping("/reconcile")
    public ApiResponse<Map<String, Object>> reconcile(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer targetType,
            @RequestParam(required = false) String settlementNo,
            @RequestParam(defaultValue = "false") boolean onlyDiff) {
        return ApiResponse.ok(settlementService.reconcile(startDate, endDate, targetType, settlementNo, onlyDiff));
    }

    @Operation(summary = "推送结算数据到ERP（兼容旧入口）")
    @PostMapping("/push-erp")
    public ApiResponse<Void> pushToErp(@RequestBody Map<String, Object> settlementData) {
        settlementService.pushToErp(settlementData);
        return ApiResponse.ok(null);
    }
}
