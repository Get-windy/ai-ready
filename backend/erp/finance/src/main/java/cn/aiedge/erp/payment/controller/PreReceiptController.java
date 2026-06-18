package cn.aiedge.erp.payment.controller;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.payment.dto.PreReceiptCreateDTO;
import cn.aiedge.erp.payment.dto.PreReceiptDTO;
import cn.aiedge.erp.payment.entity.PreReceipt;
import cn.aiedge.erp.payment.service.PreReceiptService;
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
@RequestMapping("/api/erp/pre-receipt")
@RequiredArgsConstructor
@Tag(name = "预收款管理", description = "预收款/定金创建、冲抵、没收、退还等操作")
public class PreReceiptController {

    private final PreReceiptService preReceiptService;

    @GetMapping("/page")
    @Operation(summary = "分页查询预收款单")
    public Page<PreReceiptDTO> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<PreReceipt> page = preReceiptService.pageList(keyword, customerId, status, pageNum, pageSize);
        Page<PreReceiptDTO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToDTO).collect(Collectors.toList()));
        return voPage;
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取预收款单详情")
    public PreReceiptDTO getById(@PathVariable Long id) {
        PreReceipt preReceipt = preReceiptService.getById(id);
        if (preReceipt == null) {
            throw BusinessException.notFound("预收款单不存在");
        }
        return convertToDTO(preReceipt);
    }

    @PostMapping
    @Operation(summary = "创建预收款单")
    public PreReceiptDTO create(@Valid @RequestBody PreReceiptCreateDTO dto) {
        PreReceipt preReceipt = new PreReceipt();
        BeanUtils.copyProperties(dto, preReceipt);
        preReceipt.setTenantId(1L);
        preReceipt.setCreateBy(StpUtil.getLoginIdAsLong());
        PreReceipt created = preReceiptService.createPreReceipt(preReceipt);
        return convertToDTO(created);
    }

    @PostMapping("/{id}/offset-to-receipt")
    @Operation(summary = "冲抵到收款单")
    public void offsetToReceipt(
            @PathVariable Long id,
            @Parameter(description = "收款单ID") @RequestParam Long receiptId,
            @Parameter(description = "冲抵金额") @RequestParam BigDecimal amount) {
        preReceiptService.offsetToReceipt(id, receiptId, amount);
    }

    @PostMapping("/{id}/forfeit")
    @Operation(summary = "没收定金")
    public PreReceiptDTO forfeit(
            @PathVariable Long id,
            @Parameter(description = "没收原因") @RequestParam String reason) {
        PreReceipt preReceipt = preReceiptService.forfeit(id, reason);
        return convertToDTO(preReceipt);
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "退还预收款")
    public PreReceiptDTO refund(
            @PathVariable Long id,
            @Parameter(description = "退还原因") @RequestParam String reason) {
        PreReceipt preReceipt = preReceiptService.refund(id, reason);
        return convertToDTO(preReceipt);
    }

    @GetMapping("/statistics")
    @Operation(summary = "预收款统计")
    public Map<String, Object> statistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        for (String status : new String[]{"received", "offset", "forfeited", "refunded"}) {
            stats.put(status, preReceiptService.lambdaQuery()
                    .eq(PreReceipt::getStatus, status)
                    .eq(PreReceipt::getDeleted, 0)
                    .count());
        }
        stats.put("totalAmount", preReceiptService.lambdaQuery()
                .eq(PreReceipt::getDeleted, 0)
                .list().stream()
                .map(PreReceipt::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return stats;
    }

    private PreReceiptDTO convertToDTO(PreReceipt preReceipt) {
        PreReceiptDTO dto = new PreReceiptDTO();
        BeanUtils.copyProperties(preReceipt, dto);
        return dto;
    }
}
