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

@Slf4j
@RestController
@RequestMapping("/api/erp/payment")
@RequiredArgsConstructor
@Tag(name = "付款管理", description = "付款单创建、审批、核销、完成等操作")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/page")
    @Operation(summary = "分页查询付款单")
    public Page<PaymentVO> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "供应商ID") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "订单ID") @RequestParam(required = false) Long orderId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "来源类型") @RequestParam(required = false) String sourceType,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<Payment> page = paymentService.pageList(keyword, supplierId, orderId, status, sourceType, pageNum, pageSize);
        Page<PaymentVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

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

    @GetMapping("/{id}/items")
    @Operation(summary = "获取付款明细")
    public List<PaymentItem> getItems(@PathVariable Long id) {
        return paymentService.getItems(id);
    }

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "获取供应商付款单列表")
    public List<PaymentVO> listBySupplierId(@PathVariable Long supplierId) {
        return paymentService.listBySupplierId(supplierId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "获取订单付款单列表")
    public List<PaymentVO> listByOrderId(@PathVariable Long orderId) {
        return paymentService.listByOrderId(orderId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @PostMapping
    @Operation(summary = "创建付款单")
    public PaymentVO create(@RequestBody PaymentCreateDTO dto) {
        Payment payment = new Payment();
        BeanUtils.copyProperties(dto, payment);
        payment.setTenantId(1L);
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

    @PostMapping("/from-order/{orderId}")
    @Operation(summary = "从订单创建付款单")
    public PaymentVO createFromOrder(@PathVariable Long orderId) {
        Payment payment = paymentService.createFromOrder(orderId);
        return convertToVO(payment);
    }

    @PostMapping("/from-invoice/{invoiceId}")
    @Operation(summary = "从发票创建付款单")
    public PaymentVO createFromInvoice(@PathVariable Long invoiceId) {
        Payment payment = paymentService.createFromInvoice(invoiceId);
        return convertToVO(payment);
    }

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

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public PaymentVO submitForApproval(@PathVariable Long id) {
        Payment payment = paymentService.submitForApproval(id);
        return convertToVO(payment);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public PaymentVO approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        Payment payment = paymentService.approve(id, approverId, note);
        return convertToVO(payment);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public PaymentVO reject(@PathVariable Long id, @RequestParam String reason) {
        Payment payment = paymentService.reject(id, reason);
        return convertToVO(payment);
    }

    @PostMapping("/{id}/start-verify")
    @Operation(summary = "开始核销")
    public PaymentVO startVerify(@PathVariable Long id) {
        Payment payment = paymentService.startVerify(id);
        return convertToVO(payment);
    }

    @PostMapping("/{id}/items/{itemId}/verify")
    @Operation(summary = "核销明细")
    public PaymentItem verifyItem(
            @PathVariable Long itemId,
            @RequestParam BigDecimal verifyAmount) {
        return paymentService.verifyItem(itemId, verifyAmount);
    }

    @PostMapping("/{id}/complete-verify")
    @Operation(summary = "完成核销")
    public PaymentVO completeVerify(@PathVariable Long id) {
        Payment payment = paymentService.completeVerify(id);
        return convertToVO(payment);
    }

    @PostMapping("/{id}/write-off")
    @Operation(summary = "核销付款单")
    public PaymentVO writeOff(@PathVariable Long id, @RequestBody Map<String, BigDecimal> body) {
        BigDecimal amount = body.getOrDefault("amount", BigDecimal.ZERO);
        Payment payment = paymentService.writeOff(id, amount);
        return convertToVO(payment);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成付款")
    public PaymentVO complete(@PathVariable Long id) {
        Payment payment = paymentService.complete(id);
        return convertToVO(payment);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消付款")
    public PaymentVO cancel(@PathVariable Long id, @RequestParam String reason) {
        Payment payment = paymentService.cancel(id, reason);
        return convertToVO(payment);
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "添加付款明细")
    public PaymentItem addItem(@PathVariable Long id, @RequestBody PaymentItemDTO dto) {
        PaymentItem item = new PaymentItem();
        BeanUtils.copyProperties(dto, item);
        return paymentService.addItem(id, item);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "删除付款明细")
    public void removeItem(@PathVariable Long itemId) {
        paymentService.removeItem(itemId);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除付款单")
    public boolean batchDelete(@RequestBody List<Long> ids) {
        return paymentService.removeBatchByIds(ids);
    }

    @GetMapping("/export")
    @Operation(summary = "导出付款单列表")
    public List<Payment> export(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "供应商ID") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "订单ID") @RequestParam(required = false) Long orderId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        return paymentService.exportList(keyword, supplierId, orderId, status);
    }

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