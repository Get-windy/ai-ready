package cn.aiedge.erp.payment.controller;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.payment.dto.PrePaymentCreateDTO;
import cn.aiedge.erp.payment.dto.PrePaymentDTO;
import cn.aiedge.erp.payment.dto.PrePaymentQuery;
import cn.aiedge.erp.payment.dto.PrePaymentSaveDTO;
import cn.aiedge.erp.payment.entity.PrePayment;
import cn.aiedge.erp.payment.service.PrePaymentService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@RestController
@RequestMapping("/api/erp/pre-payment")
@RequiredArgsConstructor
@Tag(name = "预付款管理", description = "预付款/定金创建、记账、冲抵、收回、退还等操作")
public class PrePaymentController {

    private final PrePaymentService prePaymentService;

    @SaCheckPermission("finance:pre-payment:list")
    @GetMapping("/next-no")
    @Operation(summary = "生成下一预付款单号")
    public String nextNo(@Parameter(description = "编号前缀") @RequestParam(required = false) String prefix) {
        return prePaymentService.generatePrePaymentNo();
    }

    @SaCheckPermission("finance:pre-payment:list")
    @GetMapping("/page")
    @Operation(summary = "分页查询预付款单")
    public Page<PrePaymentDTO> page(
            PrePaymentQuery query,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<PrePayment> page = prePaymentService.pageQuery(query, pageNum, pageSize);
        Page<PrePaymentDTO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToDTO).collect(Collectors.toList()));
        return voPage;
    }

    @SaCheckPermission("finance:pre-payment:view")
    @GetMapping("/advance-balance")
    @Operation(summary = "查询结算单位（供应商）预付余额")
    public BigDecimal advanceBalance(@Parameter(description = "供应商ID") @RequestParam Long supplierId) {
        return prePaymentService.getSupplierAdvanceBalance(supplierId);
    }

    @SaCheckPermission("finance:pre-payment:detail")
    @GetMapping("/{id}")
    @Operation(summary = "获取预付款单详情（含付款账户明细）")
    public PrePaymentDTO getById(@PathVariable Long id) {
        return prePaymentService.getDetail(id);
    }

    @SaCheckPermission("finance:pre-payment:create")
    @PostMapping("/save")
    @Operation(summary = "保存预付款单草稿（含付款账户明细）")
    public PrePaymentDTO save(@Valid @RequestBody PrePaymentSaveDTO dto) {
        PrePayment prePayment = prePaymentService.saveDraft(dto);
        return convertToDTO(prePayment);
    }

    @SaCheckPermission("finance:pre-payment:create")
    @PostMapping("/confirm")
    @Operation(summary = "预付款单记账（预付余额增加 + 生成凭证）")
    public PrePaymentDTO confirm(
            @Parameter(description = "单据ID") @RequestParam Long id,
            @Parameter(description = "记账人ID") @RequestParam(required = false) Long operatorId,
            @Parameter(description = "记账人") @RequestParam(required = false) String operatorName) {
        Long bookkeeperId = operatorId != null ? operatorId : StpUtil.getLoginIdAsLong();
        PrePayment prePayment = prePaymentService.confirm(id, bookkeeperId, operatorName);
        return convertToDTO(prePayment);
    }

    @SaCheckPermission("finance:pre-payment:create")
    @PostMapping
    @Operation(summary = "创建预付款单（旧版）")
    public PrePaymentDTO create(@Valid @RequestBody PrePaymentCreateDTO dto) {
        PrePayment prePayment = new PrePayment();
        BeanUtils.copyProperties(dto, prePayment);
        prePayment.setCreateBy(StpUtil.getLoginIdAsLong());
        PrePayment created = prePaymentService.createPrePayment(prePayment);
        return convertToDTO(created);
    }

    @SaCheckPermission("finance:pre-payment:create")
    @PostMapping("/{id}/offset-to-payment")
    @Operation(summary = "冲抵到付款单")
    public void offsetToPayment(
            @PathVariable Long id,
            @Parameter(description = "付款单ID") @RequestParam Long paymentId,
            @Parameter(description = "冲抵金额") @RequestParam BigDecimal amount) {
        prePaymentService.offsetToPayment(id, paymentId, amount);
    }

    @SaCheckPermission("finance:pre-payment:create")
    @PostMapping("/{id}/recover")
    @Operation(summary = "收回预付款(供应商违约)")
    public PrePaymentDTO recover(
            @PathVariable Long id,
            @Parameter(description = "收回原因") @RequestParam String reason) {
        PrePayment prePayment = prePaymentService.recover(id, reason);
        return convertToDTO(prePayment);
    }

    @SaCheckPermission("finance:pre-payment:create")
    @PostMapping("/{id}/refund")
    @Operation(summary = "退还预付款(供应商退款)")
    public PrePaymentDTO refund(
            @PathVariable Long id,
            @Parameter(description = "退还原因") @RequestParam String reason) {
        PrePayment prePayment = prePaymentService.refund(id, reason);
        return convertToDTO(prePayment);
    }

    @SaCheckPermission("finance:pre-payment:view")
    @GetMapping("/statistics")
    @Operation(summary = "预付款统计")
    public Map<String, Object> statistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        for (String status : new String[]{"draft", "confirmed", "offset", "recovered", "refunded"}) {
            stats.put(status, prePaymentService.lambdaQuery()
                    .eq(PrePayment::getStatus, status)
                    .eq(PrePayment::getDeleted, 0)
                    .count());
        }
        stats.put("totalAmount", prePaymentService.lambdaQuery()
                .eq(PrePayment::getDeleted, 0)
                .list().stream()
                .map(PrePayment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return stats;
    }

    private PrePaymentDTO convertToDTO(PrePayment prePayment) {
        PrePaymentDTO dto = new PrePaymentDTO();
        BeanUtils.copyProperties(prePayment, dto);
        return dto;
    }
}
