package cn.aiedge.erp.payment.controller;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.payment.dto.PreReceiptCreateDTO;
import cn.aiedge.erp.payment.dto.PreReceiptDTO;
import cn.aiedge.erp.payment.dto.PreReceiptQuery;
import cn.aiedge.erp.payment.dto.PreReceiptSaveDTO;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@RestController
@RequestMapping("/api/erp/pre-receipt")
@RequiredArgsConstructor
@Tag(name = "预收款管理", description = "预收款/定金创建、记账、冲抵、没收、退还等操作")
public class PreReceiptController {

    private final PreReceiptService preReceiptService;

    @SaCheckPermission("finance:pre-receipt:list")
    @GetMapping("/next-no")
    @Operation(summary = "生成下一预收款单号")
    public String nextNo(@Parameter(description = "编号前缀") @RequestParam(required = false) String prefix) {
        return preReceiptService.generatePreReceiptNo();
    }

    @SaCheckPermission("finance:pre-receipt:list")
    @GetMapping("/page")
    @Operation(summary = "分页查询预收款单")
    public Page<PreReceiptDTO> page(
            PreReceiptQuery query,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<PreReceipt> page = preReceiptService.pageQuery(query, pageNum, pageSize);
        Page<PreReceiptDTO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToDTO).collect(Collectors.toList()));
        return voPage;
    }

    @SaCheckPermission("finance:pre-receipt:view")
    @GetMapping("/advance-balance")
    @Operation(summary = "查询结算单位（客户）预收余额")
    public BigDecimal advanceBalance(@Parameter(description = "客户ID") @RequestParam Long customerId) {
        return preReceiptService.getCustomerAdvanceBalance(customerId);
    }

    @SaCheckPermission("finance:pre-receipt:detail")
    @GetMapping("/{id}")
    @Operation(summary = "获取预收款单详情（含收款账户明细）")
    public PreReceiptDTO getById(@PathVariable Long id) {
        return preReceiptService.getDetail(id);
    }

    @SaCheckPermission("finance:pre-receipt:create")
    @PostMapping("/save")
    @Operation(summary = "保存预收款单草稿（含收款账户明细）")
    public PreReceiptDTO save(@Valid @RequestBody PreReceiptSaveDTO dto) {
        PreReceipt preReceipt = preReceiptService.saveDraft(dto);
        return convertToDTO(preReceipt);
    }

    @SaCheckPermission("finance:pre-receipt:create")
    @PostMapping("/confirm")
    @Operation(summary = "预收款单记账（预收余额增加 + 生成凭证）")
    public PreReceiptDTO confirm(
            @Parameter(description = "单据ID") @RequestParam Long id,
            @Parameter(description = "记账人ID") @RequestParam(required = false) Long operatorId,
            @Parameter(description = "记账人") @RequestParam(required = false) String operatorName) {
        Long bookkeeperId = operatorId != null ? operatorId : StpUtil.getLoginIdAsLong();
        PreReceipt preReceipt = preReceiptService.confirm(id, bookkeeperId, operatorName);
        return convertToDTO(preReceipt);
    }

    @SaCheckPermission("finance:pre-receipt:create")
    @PostMapping
    @Operation(summary = "创建预收款单（旧版）")
    public PreReceiptDTO create(@Valid @RequestBody PreReceiptCreateDTO dto) {
        PreReceipt preReceipt = new PreReceipt();
        BeanUtils.copyProperties(dto, preReceipt);
        preReceipt.setTenantId(1L);
        preReceipt.setCreateBy(StpUtil.getLoginIdAsLong());
        PreReceipt created = preReceiptService.createPreReceipt(preReceipt);
        return convertToDTO(created);
    }

    @SaCheckPermission("finance:pre-receipt:create")
    @PostMapping("/batch-confirm")
    @Operation(summary = "批量确认预收款（到账入账）")
    public int batchConfirm(@Parameter(description = "预收款ID列表") @RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.badRequest("请选择要确认的预收款单");
        }
        Long operatorId = StpUtil.getLoginIdAsLong();
        int count = 0;
        for (Long id : ids) {
            preReceiptService.confirm(id, operatorId, null);
            count++;
        }
        return count;
    }

    @SaCheckPermission("finance:pre-receipt:create")
    @PostMapping("/{id}/offset-to-receipt")
    @Operation(summary = "冲抵到收款单")
    public void offsetToReceipt(
            @PathVariable Long id,
            @Parameter(description = "收款单ID") @RequestParam Long receiptId,
            @Parameter(description = "冲抵金额") @RequestParam BigDecimal amount) {
        preReceiptService.offsetToReceipt(id, receiptId, amount);
    }

    @SaCheckPermission("finance:pre-receipt:create")
    @PostMapping("/{id}/forfeit")
    @Operation(summary = "没收定金")
    public PreReceiptDTO forfeit(
            @PathVariable Long id,
            @Parameter(description = "没收原因") @RequestParam String reason) {
        PreReceipt preReceipt = preReceiptService.forfeit(id, reason);
        return convertToDTO(preReceipt);
    }

    @SaCheckPermission("finance:pre-receipt:create")
    @PostMapping("/{id}/refund")
    @Operation(summary = "退还预收款")
    public PreReceiptDTO refund(
            @PathVariable Long id,
            @Parameter(description = "退还原因") @RequestParam String reason) {
        PreReceipt preReceipt = preReceiptService.refund(id, reason);
        return convertToDTO(preReceipt);
    }

    @SaCheckPermission("finance:pre-receipt:view")
    @GetMapping("/statistics")
    @Operation(summary = "预收款统计")
    public Map<String, Object> statistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        for (String status : new String[]{"draft", "confirmed", "received", "offset", "forfeited", "refunded"}) {
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
