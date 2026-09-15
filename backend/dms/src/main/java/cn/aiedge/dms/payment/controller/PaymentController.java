package cn.aiedge.dms.payment.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.payment.dto.DmsPaymentVO;
import cn.aiedge.dms.payment.dto.PaymentQueryDTO;
import cn.aiedge.dms.payment.entity.DmsPayment;
import cn.aiedge.dms.payment.entity.DmsPaymentCollection;
import cn.aiedge.dms.payment.entity.DmsPaymentFlow;
import cn.aiedge.dms.payment.service.PaymentCollectionService;
import cn.aiedge.dms.payment.service.PaymentFlowService;
import cn.aiedge.dms.payment.service.PaymentService;
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
 * 末端收款控制器（配送 → 结算收款 → 收款管理，菜单 80920）
 *
 * <p>口径（《收款管理开发文档》§3.2）：<b>收款类型</b> `paymentType`（1 代收货款 / 2 配送费）与
 * <b>支付方式</b> `payChannel`（微信/支付宝/现金/POS/银行/其他）分离；收款码配置化；回调幂等；
 * 交款稽核（应上交 vs 已上交 + 超时预警）；未付催收/核销；支付流水对账；推送财务幂等。</p>
 */
@Tag(name = "末端收款")
@RestController
@RequestMapping("/api/dms/payment")
@RequiredArgsConstructor
@SaCheckLogin
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentCollectionService collectionService;
    private final PaymentFlowService flowService;

    // ═══ 字典 / 收款码 ═══

    @Operation(summary = "收款字典（支付方式 / 收款类型 / 收款码是否开通 / 现金限额与交款时限）")
    @GetMapping("/dict")
    public ApiResponse<Map<String, Object>> dict() {
        return ApiResponse.ok(paymentService.dict());
    }

    @Operation(summary = "生成收款二维码（未配置收款码服务时返回空 URL，不生成假二维码）")
    @PostMapping("/qrcode")
    public ApiResponse<DmsPayment> generateQrcode(
            @Parameter(description = "任务ID") @RequestParam Long taskId,
            @Parameter(description = "收款金额") @RequestParam BigDecimal amount) {
        return ApiResponse.ok(paymentService.generateQrcode(taskId, amount));
    }

    // ═══ 收款 ═══

    @Operation(summary = "线下收款确认（现金/POS/银行转账；现金超限额拒绝）")
    @PostMapping("/confirm")
    public ApiResponse<DmsPayment> confirmPayment(
            @Parameter(description = "任务ID") @RequestParam Long taskId,
            @Parameter(description = "支付方式 1-微信 2-支付宝 3-现金 4-POS 5-银行转账 9-其他") @RequestParam(required = false) Integer payChannel,
            @Parameter(description = "收款金额") @RequestParam BigDecimal amount,
            @Parameter(description = "外部单号") @RequestParam(required = false) String externalOrderNo,
            @Parameter(description = "收款类型 1-代收货款 2-配送费（为空按任务代收货款推断）") @RequestParam(required = false) Integer paymentType) {
        return ApiResponse.ok(paymentService.confirmPayment(taskId, payChannel, amount, externalOrderNo, paymentType));
    }

    @OperationLog(module = "收款管理", type = "CREATE", desc = "批量线下收款确认")
    @Operation(summary = "批量线下收款确认（逐单反馈）")
    @PostMapping("/confirm-batch")
    public ApiResponse<Map<String, Object>> confirmBatch(@RequestBody List<Map<String, Object>> rows) {
        return ApiResponse.ok(paymentService.confirmBatch(rows));
    }

    @Operation(summary = "支付回调（验签后可接；幂等：重复回调不重复置账）")
    @PostMapping("/callback")
    public ApiResponse<Map<String, Object>> callback(
            @Parameter(description = "任务ID") @RequestParam Long taskId,
            @Parameter(description = "平台交易号（幂等键）") @RequestParam(required = false) String tradeNo,
            @Parameter(description = "支付方式") @RequestParam(required = false) Integer payChannel,
            @Parameter(description = "实收金额") @RequestParam(required = false) BigDecimal amount,
            @Parameter(description = "外部单号") @RequestParam(required = false) String externalOrderNo) {
        return ApiResponse.ok(paymentService.callback(taskId, tradeNo, payChannel, amount, externalOrderNo));
    }

    @Operation(summary = "标记未付（挂账）")
    @PostMapping("/mark-unpaid")
    public ApiResponse<Void> markUnpaid(
            @Parameter(description = "任务ID") @RequestParam Long taskId,
            @Parameter(description = "未付原因（必填）") @RequestParam String remark) {
        paymentService.markUnpaid(taskId, remark);
        return ApiResponse.ok(null);
    }

    // ═══ 台账 / 统计 ═══

    @Operation(summary = "收款台账分页（联查任务/客户/配送员 + 催收/超时派生列）")
    @GetMapping("/page")
    public ApiResponse<IPage<DmsPaymentVO>> page(PaymentQueryDTO query) {
        return ApiResponse.ok(paymentService.page(query));
    }

    @OperationLog(module = "收款管理", type = "EXPORT", desc = "收款台账导出（含权限审计）")
    @Operation(summary = "收款台账导出数据（前端据此生成真实 xlsx）")
    @GetMapping("/export")
    public ApiResponse<List<DmsPaymentVO>> export(PaymentQueryDTO query) {
        return ApiResponse.ok(paymentService.exportList(query));
    }

    @Operation(summary = "收款统计（笔数/金额/未付/代收货款/配送费/已上交/超时未交）")
    @GetMapping("/stat")
    public ApiResponse<Map<String, Object>> stat(PaymentQueryDTO query) {
        return ApiResponse.ok(paymentService.stat(query));
    }

    // ═══ 资金上交 / 稽核 ═══

    @OperationLog(module = "收款管理", type = "UPDATE", desc = "交款登记（资金上交）")
    @Operation(summary = "交款登记（配送员上交企业）")
    @PostMapping("/{id}/handover")
    public ApiResponse<DmsPayment> handover(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String remark) {
        return ApiResponse.ok(paymentService.handover(id, amount, operatorId, operatorName, remark));
    }

    @Operation(summary = "交款稽核汇总（按配送员：应上交 vs 已上交 vs 未上交 + 超时预警）")
    @GetMapping("/handover/summary")
    public ApiResponse<Map<String, Object>> handoverSummary(
            @RequestParam(required = false) Long riderId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(paymentService.handoverSummary(riderId, startDate, endDate));
    }

    // ═══ 未付管理（挂账 → 催收 → 核销） ═══

    @Operation(summary = "未付（挂账）台账分页")
    @GetMapping("/unpaid/page")
    public ApiResponse<IPage<DmsPaymentVO>> unpaidPage(PaymentQueryDTO query) {
        return ApiResponse.ok(collectionService.unpaidPage(query.getTaskNo(), query.getCustomerName(),
                query.getRiderId(), parseDate(query.getStartDate()), parseDate(query.getEndDate()),
                query.getCurrent(), query.getSize()));
    }

    @Operation(summary = "未付（挂账）汇总")
    @GetMapping("/unpaid/stat")
    public ApiResponse<Map<String, Object>> unpaidStat(PaymentQueryDTO query) {
        return ApiResponse.ok(collectionService.unpaidStat(query.getRiderId(),
                parseDate(query.getStartDate()), parseDate(query.getEndDate())));
    }

    @OperationLog(module = "收款管理", type = "UPDATE", desc = "未付催收登记")
    @Operation(summary = "催收登记（挂账单据催收留痕）")
    @PostMapping("/{id}/urge")
    public ApiResponse<Map<String, Object>> urge(
            @PathVariable Long id,
            @RequestParam String content,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate promiseDate,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String operatorName) {
        return ApiResponse.ok(collectionService.urge(id, content, promiseDate, operatorId, operatorName));
    }

    @Operation(summary = "承诺付款日登记")
    @PostMapping("/{id}/promise")
    public ApiResponse<DmsPaymentCollection> promise(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate promiseDate,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String operatorName) {
        return ApiResponse.ok(collectionService.promise(id, promiseDate, content, operatorId, operatorName));
    }

    @OperationLog(module = "收款管理", type = "UPDATE", desc = "挂账核销（收款到账）")
    @Operation(summary = "核销（挂账收回，累计收清自动置已支付）")
    @PostMapping("/{id}/write-off")
    public ApiResponse<Map<String, Object>> writeOff(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) Integer payChannel,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String operatorName) {
        return ApiResponse.ok(collectionService.writeOff(id, amount, payChannel, content, operatorId, operatorName));
    }

    @Operation(summary = "挂账动作流水（催收/承诺/核销）")
    @GetMapping("/{id}/collections")
    public ApiResponse<List<DmsPaymentCollection>> collections(@PathVariable Long id) {
        return ApiResponse.ok(collectionService.collections(id));
    }

    // ═══ 财务打通（推 ERP 生成收款单 / 核销应收） ═══

    @OperationLog(module = "收款管理", type = "UPDATE", desc = "推送财务（收款单/应收核销）")
    @Operation(summary = "推送财务（幂等：重复推送返回既有 traceId）")
    @PostMapping("/push-finance")
    public ApiResponse<Map<String, Object>> pushFinance(
            @Parameter(description = "收款记录ID（单个）") @RequestParam(required = false) Long paymentId,
            @Parameter(description = "收款记录ID列表（批量）") @RequestParam(required = false) List<Long> paymentIds) {
        if (paymentIds != null && !paymentIds.isEmpty()) {
            return ApiResponse.ok(paymentService.pushFinanceBatch(paymentIds));
        }
        if (paymentId == null) {
            throw new cn.aiedge.dms.common.exception.DmsBusinessException("请指定 paymentId 或 paymentIds");
        }
        return ApiResponse.ok(paymentService.pushFinance(paymentId));
    }

    // ═══ 支付流水 / 对账 ═══

    @Operation(summary = "支付平台流水分页")
    @GetMapping("/flow/page")
    public ApiResponse<IPage<DmsPaymentFlow>> flowPage(
            @RequestParam(required = false) String channelCode,
            @RequestParam(required = false) String tradeNo,
            @RequestParam(required = false) Integer matchStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(flowService.page(channelCode, tradeNo, matchStatus, startDate, endDate, current, size));
    }

    @Operation(summary = "支付平台流水统计（已匹配/未匹配/差异）")
    @GetMapping("/flow/stat")
    public ApiResponse<Map<String, Object>> flowStat(
            @RequestParam(required = false) String channelCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(flowService.stat(channelCode, startDate, endDate));
    }

    @OperationLog(module = "收款管理", type = "IMPORT", desc = "支付平台流水导入")
    @Operation(summary = "支付平台流水批量导入（同渠道同交易号幂等跳过）")
    @PostMapping("/flow/import")
    public ApiResponse<Map<String, Object>> flowImport(
            @RequestBody List<Map<String, Object>> rows,
            @RequestParam(required = false) String defaultChannel) {
        return ApiResponse.ok(flowService.importFlows(rows, defaultChannel));
    }

    @OperationLog(module = "收款管理", type = "OTHER", desc = "支付流水对账")
    @Operation(summary = "与支付平台流水对账（逐笔匹配，输出三类差异）")
    @PostMapping("/reconcile")
    public ApiResponse<Map<String, Object>> reconcile(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String channelCode) {
        return ApiResponse.ok(flowService.reconcile(startDate, endDate, channelCode));
    }

    @Operation(summary = "流水人工匹配到收款记录")
    @PostMapping("/flow/{id}/match")
    public ApiResponse<DmsPaymentFlow> flowMatch(@PathVariable Long id, @RequestParam Long paymentId) {
        return ApiResponse.ok(flowService.manualMatch(id, paymentId));
    }

    @Operation(summary = "忽略流水差异（渠道测试单/误报）")
    @PostMapping("/flow/{id}/ignore")
    public ApiResponse<DmsPaymentFlow> flowIgnore(
            @PathVariable Long id, @RequestParam(required = false) String remark) {
        return ApiResponse.ok(flowService.ignore(id, remark));
    }

    // ═══ 兼容旧入口 ═══

    @Operation(summary = "按任务查收款记录")
    @GetMapping("/{taskId}")
    public ApiResponse<DmsPayment> getByTaskId(@PathVariable Long taskId) {
        return ApiResponse.ok(paymentService.getByTaskId(taskId));
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
