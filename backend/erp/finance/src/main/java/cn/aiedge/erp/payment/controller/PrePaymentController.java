package cn.aiedge.erp.payment.controller;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.payment.dto.PrePaymentCreateDTO;
import cn.aiedge.erp.payment.dto.PrePaymentDTO;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/erp/pre-payment")
@RequiredArgsConstructor
@Tag(name = "预付款管理", description = "预付款/定金创建、冲抵、收回、退还等操作")
public class PrePaymentController {

    private final PrePaymentService prePaymentService;

    @GetMapping("/page")
    @Operation(summary = "分页查询预付款单")
    public Page<PrePaymentDTO> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "供应商ID") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<PrePayment> page = prePaymentService.pageList(keyword, supplierId, status, pageNum, pageSize);
        Page<PrePaymentDTO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToDTO).collect(Collectors.toList()));
        return voPage;
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取预付款单详情")
    public PrePaymentDTO getById(@PathVariable Long id) {
        PrePayment prePayment = prePaymentService.getById(id);
        if (prePayment == null) {
            throw BusinessException.notFound("预付款单不存在");
        }
        return convertToDTO(prePayment);
    }

    @PostMapping
    @Operation(summary = "创建预付款单")
    public PrePaymentDTO create(@Valid @RequestBody PrePaymentCreateDTO dto) {
        PrePayment prePayment = new PrePayment();
        BeanUtils.copyProperties(dto, prePayment);
        prePayment.setTenantId(1L);
        prePayment.setCreateBy(StpUtil.getLoginIdAsLong());
        PrePayment created = prePaymentService.createPrePayment(prePayment);
        return convertToDTO(created);
    }

    @PostMapping("/{id}/offset-to-payment")
    @Operation(summary = "冲抵到付款单")
    public void offsetToPayment(
            @PathVariable Long id,
            @Parameter(description = "付款单ID") @RequestParam Long paymentId,
            @Parameter(description = "冲抵金额") @RequestParam BigDecimal amount) {
        prePaymentService.offsetToPayment(id, paymentId, amount);
    }

    @PostMapping("/{id}/recover")
    @Operation(summary = "收回预付款(供应商违约)")
    public PrePaymentDTO recover(
            @PathVariable Long id,
            @Parameter(description = "收回原因") @RequestParam String reason) {
        PrePayment prePayment = prePaymentService.recover(id, reason);
        return convertToDTO(prePayment);
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "退还预付款(供应商退款)")
    public PrePaymentDTO refund(
            @PathVariable Long id,
            @Parameter(description = "退还原因") @RequestParam String reason) {
        PrePayment prePayment = prePaymentService.refund(id, reason);
        return convertToDTO(prePayment);
    }

    @GetMapping("/statistics")
    @Operation(summary = "预付款统计")
    public Map<String, Object> statistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        for (String status : new String[]{"paid", "offset", "recovered", "refunded"}) {
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
