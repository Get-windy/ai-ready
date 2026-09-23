package cn.aiedge.erp.payment.controller;

import cn.aiedge.erp.payment.dto.PaymentCreateDTO;
import cn.aiedge.erp.payment.dto.PaymentItemDTO;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.payment.dto.PaymentVO;
import cn.aiedge.erp.payment.entity.Payment;
import cn.aiedge.erp.payment.entity.PaymentItem;
import cn.aiedge.erp.payment.enums.ReceiptStatus;
import cn.aiedge.erp.payment.service.PaymentService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@RestController
@RequestMapping("/api/erp/payment")
@RequiredArgsConstructor
@Tag(name = "付款管理", description = "付款单创建、审批、核销、完成等操作")
public class PaymentController {

    private final PaymentService paymentService;

    @SaCheckPermission("finance:payment:list")
    @GetMapping("/page")
    @Operation(summary = "分页查询付款单")
    public Page<PaymentVO> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "供应商ID") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "订单ID") @RequestParam(required = false) Long orderId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "来源类型") @RequestParam(required = false) String sourceType,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "结算单位") @RequestParam(required = false) String supplierName,
            @Parameter(description = "经手人") @RequestParam(required = false) String handlerName,
            @Parameter(description = "部门") @RequestParam(required = false) String departmentName,
            @Parameter(description = "制单人") @RequestParam(required = false) String creatorName,
            @Parameter(description = "记账人") @RequestParam(required = false) String bookkeeperName,
            @Parameter(description = "单据备注") @RequestParam(required = false) String remark,
            @Parameter(description = "多状态(逗号分隔)") @RequestParam(required = false) String statuses,
            @Parameter(description = "单据编号") @RequestParam(required = false) String paymentNo,
            @Parameter(description = "来源订单编号") @RequestParam(required = false) String orderNo,
            @Parameter(description = "配送任务编号") @RequestParam(required = false) String deliveryNo,
            @Parameter(description = "付款账户1") @RequestParam(required = false) String paymentAccount1,
            @Parameter(description = "付款账户2") @RequestParam(required = false) String paymentAccount2,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<Payment> page = paymentService.pageList(keyword, supplierId, orderId, status, sourceType,
                startDate, endDate, supplierName, handlerName, departmentName, creatorName, bookkeeperName, remark,
                statuses, paymentNo, orderNo, deliveryNo, paymentAccount1, paymentAccount2, pageNum, pageSize);
        Page<PaymentVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    @SaCheckPermission("finance:payment:view")
    @GetMapping("/page-detail")
    @Operation(summary = "按明细付款单分页查询（付款明细 tab）")
    public Page<cn.aiedge.erp.payment.dto.PaymentItemDetailVO> pageDetail(
            @Parameter(description = "付款单ID") @RequestParam(required = false) Long paymentId,
            @Parameter(description = "供应商ID") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "往来单位") @RequestParam(required = false) String tradeUnit,
            @Parameter(description = "源单经手人") @RequestParam(required = false) String sourceHandler,
            @Parameter(description = "结算单据编号") @RequestParam(required = false) String settlementNo,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return paymentService.pageDetail(paymentId, supplierId, keyword, status, tradeUnit, sourceHandler,
                settlementNo, startDate, endDate, pageNum, pageSize);
    }

    @SaCheckPermission("finance:payment:list")
    @GetMapping("/next-no")
    @Operation(summary = "生成付款单号（FKD- 前缀）")
    public String nextNo() {
        return paymentService.nextNo();
    }

    @SaCheckPermission("finance:payment:detail")
    @GetMapping("/{id}")
    @Operation(summary = "获取付款单详情")
    public PaymentVO getById(@PathVariable Long id) {
        Payment payment = paymentService.getById(id);
        if (payment == null) {
            throw BusinessException.notFound("付款单不存在");
        }
        PaymentVO vo = convertToVO(payment);
        vo.setItems(paymentService.getItems(id));
        return vo;
    }

    @SaCheckPermission("finance:payment:list")
    @GetMapping("/{id}/items")
    @Operation(summary = "获取付款明细")
    public List<PaymentItem> getItems(@PathVariable Long id) {
        return paymentService.getItems(id);
    }

    @SaCheckPermission("finance:payment:detail")
    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "获取供应商付款单列表")
    public List<PaymentVO> listBySupplierId(@PathVariable Long supplierId) {
        return paymentService.listBySupplierId(supplierId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @SaCheckPermission("finance:payment:detail")
    @GetMapping("/order/{orderId}")
    @Operation(summary = "获取订单付款单列表")
    public List<PaymentVO> listByOrderId(@PathVariable Long orderId) {
        return paymentService.listByOrderId(orderId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @SaCheckPermission("finance:payment:create")
    @PostMapping
    @Operation(summary = "创建付款单")
    public PaymentVO create(@RequestBody PaymentCreateDTO dto) {
        Payment payment = new Payment();
        BeanUtils.copyProperties(dto, payment);
        payment.setCreateBy(StpUtil.getLoginIdAsLong());
        List<PaymentItem> items = null;
        if (dto.getItems() != null) {
            items = dto.getItems().stream().map(itemDTO -> {
                PaymentItem item = new PaymentItem();
                BeanUtils.copyProperties(itemDTO, item);
                return item;
            }).collect(Collectors.toList());
        }
        Payment created = paymentService.createPayment(payment, items);
        return convertToVO(created);
    }

    @SaCheckPermission("finance:payment:create")
    @PostMapping("/from-order/{orderId}")
    @Operation(summary = "从订单创建付款单")
    public PaymentVO createFromOrder(@PathVariable Long orderId) {
        Payment payment = paymentService.createFromOrder(orderId);
        return convertToVO(payment);
    }

    @SaCheckPermission("finance:payment:create")
    @PostMapping("/from-invoice/{invoiceId}")
    @Operation(summary = "从发票创建付款单")
    public PaymentVO createFromInvoice(@PathVariable Long invoiceId) {
        Payment payment = paymentService.createFromInvoice(invoiceId);
        return convertToVO(payment);
    }

    @SaCheckPermission("finance:payment:update")
    @PutMapping("/{id}")
    @Operation(summary = "更新付款单")
    public PaymentVO update(@PathVariable Long id, @RequestBody PaymentCreateDTO dto) {
        Payment payment = new Payment();
        BeanUtils.copyProperties(dto, payment);
        List<PaymentItem> items = null;
        if (dto.getItems() != null) {
            items = dto.getItems().stream().map(itemDTO -> {
                PaymentItem item = new PaymentItem();
                BeanUtils.copyProperties(itemDTO, item);
                return item;
            }).collect(Collectors.toList());
        }
        Payment updated = paymentService.updatePayment(id, payment, items);
        return convertToVO(updated);
    }

    @SaCheckPermission("finance:payment:submit")
    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public PaymentVO submitForApproval(@PathVariable Long id) {
        Payment payment = paymentService.submitForApproval(id);
        return convertToVO(payment);
    }

    @SaCheckPermission("finance:payment:approve")
    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public PaymentVO approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        Payment payment = paymentService.approve(id, approverId, note);
        return convertToVO(payment);
    }

    @SaCheckPermission("finance:payment:approve")
    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public PaymentVO reject(@PathVariable Long id, @RequestParam String reason) {
        Payment payment = paymentService.reject(id, reason);
        return convertToVO(payment);
    }

    @SaCheckPermission("finance:payment:create")
    @PostMapping("/{id}/start-verify")
    @Operation(summary = "开始核销")
    public PaymentVO startVerify(@PathVariable Long id) {
        Payment payment = paymentService.startVerify(id);
        return convertToVO(payment);
    }

    @SaCheckPermission("finance:payment:create")
    @PostMapping("/{id}/items/{itemId}/verify")
    @Operation(summary = "核销明细")
    public PaymentItem verifyItem(
            @PathVariable Long itemId,
            @RequestParam BigDecimal verifyAmount) {
        return paymentService.verifyItem(itemId, verifyAmount);
    }

    @SaCheckPermission("finance:payment:create")
    @PostMapping("/{id}/complete-verify")
    @Operation(summary = "完成核销")
    public PaymentVO completeVerify(@PathVariable Long id) {
        Payment payment = paymentService.completeVerify(id);
        return convertToVO(payment);
    }

    @SaCheckPermission("finance:payment:create")
    @PostMapping("/{id}/write-off")
    @Operation(summary = "核销付款单")
    public PaymentVO writeOff(@PathVariable Long id, @RequestBody Map<String, BigDecimal> body) {
        BigDecimal amount = body.getOrDefault("amount", BigDecimal.ZERO);
        Payment payment = paymentService.writeOff(id, amount);
        return convertToVO(payment);
    }

    @SaCheckPermission("finance:payment:create")
    @PostMapping("/{id}/complete")
    @Operation(summary = "完成付款")
    public PaymentVO complete(@PathVariable Long id) {
        Payment payment = paymentService.complete(id);
        return convertToVO(payment);
    }

    @SaCheckPermission("finance:payment:create")
    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消付款")
    public PaymentVO cancel(@PathVariable Long id, @RequestParam String reason) {
        Payment payment = paymentService.cancel(id, reason);
        return convertToVO(payment);
    }

    @SaCheckPermission("finance:payment:create")
    @PostMapping("/{id}/items")
    @Operation(summary = "添加付款明细")
    public PaymentItem addItem(@PathVariable Long id, @RequestBody PaymentItemDTO dto) {
        PaymentItem item = new PaymentItem();
        BeanUtils.copyProperties(dto, item);
        return paymentService.addItem(id, item);
    }

    @SaCheckPermission("finance:payment:delete")
    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "删除付款明细")
    public void removeItem(@PathVariable Long itemId) {
        paymentService.removeItem(itemId);
    }

    @SaCheckPermission("finance:payment:delete")
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除付款单")
    public boolean batchDelete(@RequestBody List<Long> ids) {
        return paymentService.removeBatchByIds(ids);
    }

    @SaCheckPermission("finance:payment:create")
    @PostMapping("/batch-print")
    @Operation(summary = "批量打印付款单")
    public Map<String, Object> batchPrint(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) body.getOrDefault("ids", new ArrayList<>());
        String template = (String) body.getOrDefault("template", "default");
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("count", ids == null ? 0 : ids.size());
        result.put("template", template);
        return result;
    }

    @SaCheckPermission("finance:payment:export")
    @GetMapping("/export")
    @Operation(summary = "导出付款单列表")
    public List<Payment> export(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "供应商ID") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "订单ID") @RequestParam(required = false) Long orderId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        return paymentService.exportList(keyword, supplierId, orderId, status);
    }

    @SaCheckPermission("finance:payment:view")
    @GetMapping("/statistics")
    @Operation(summary = "付款统计")
    public Map<String, Object> statistics() {
        Map<String, Object> stats = new HashMap<>();
        for (ReceiptStatus status : ReceiptStatus.values()) {
            stats.put(status.getDesc(), paymentService.lambdaQuery()
                    .eq(Payment::getStatus, status.getCode())
                    .eq(Payment::getDeleted, 0)
                    .count());
        }
        List<Payment> completedPayments = paymentService.lambdaQuery()
                .eq(Payment::getStatus, ReceiptStatus.COMPLETED.getCode())
                .eq(Payment::getDeleted, 0)
                .list();
        BigDecimal totalPaymentAmount = completedPayments.stream()
                .map(Payment::getPaymentAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVerifiedAmount = completedPayments.stream()
                .map(Payment::getVerifiedAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalPaymentAmount", totalPaymentAmount);
        stats.put("totalVerifiedAmount", totalVerifiedAmount);
        return stats;
    }

    private PaymentVO convertToVO(Payment payment) {
        PaymentVO vo = new PaymentVO();
        BeanUtils.copyProperties(payment, vo);
        for (ReceiptStatus status : ReceiptStatus.values()) {
            if (status.getCode().equals(payment.getStatus())) {
                vo.setStatusDesc(status.getDesc());
                break;
            }
        }
        return vo;
    }
}