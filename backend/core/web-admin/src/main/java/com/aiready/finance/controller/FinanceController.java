package com.aiready.finance.controller;

import com.aiready.finance.dto.*;
import com.aiready.finance.service.FinanceService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 财务管理控制器
 */
@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
@Tag(name = "财务管理", description = "财务管理接口（科目、凭证、账簿、报表）")
public class FinanceController {

    private final FinanceService financeService;

    // ==================== 会计科目管理 ====================

    @PostMapping("/subjects")
    @Operation(summary = "创建会计科目")
    public AccountSubjectDTO createSubject(@Valid @RequestBody AccountSubjectSaveRequest request,
                                           @RequestParam Long operatorId) {
        return financeService.createSubject(request, operatorId);
    }

    @PutMapping("/subjects/{subjectId}")
    @Operation(summary = "更新会计科目")
    public AccountSubjectDTO updateSubject(@PathVariable Long subjectId,
                                           @Valid @RequestBody AccountSubjectSaveRequest request,
                                           @RequestParam Long operatorId) {
        return financeService.updateSubject(subjectId, request, operatorId);
    }

    @DeleteMapping("/subjects/{subjectId}")
    @Operation(summary = "删除会计科目")
    public void deleteSubject(@PathVariable Long subjectId,
                              @RequestParam Long operatorId) {
        financeService.deleteSubject(subjectId, operatorId);
    }

    @PostMapping("/subjects/batch-delete")
    @Operation(summary = "批量删除会计科目")
    public void batchDeleteSubjects(@RequestBody List<Long> subjectIds,
                                    @RequestParam Long operatorId) {
        financeService.batchDeleteSubjects(subjectIds, operatorId);
    }

    @GetMapping("/subjects/{subjectId}")
    @Operation(summary = "获取会计科目详情")
    public AccountSubjectDTO getSubjectById(@PathVariable Long subjectId) {
        return financeService.getSubjectById(subjectId);
    }

    @GetMapping("/subjects/code/{subjectCode}")
    @Operation(summary = "根据编码获取会计科目")
    public AccountSubjectDTO getSubjectByCode(@PathVariable String subjectCode) {
        return financeService.getSubjectByCode(subjectCode);
    }

    @PostMapping("/subjects/list")
    @Operation(summary = "查询会计科目列表")
    public IPage<AccountSubjectDTO> querySubjects(@RequestBody AccountSubjectQueryRequest request) {
        return financeService.querySubjects(request);
    }

    @GetMapping("/subjects/tree")
    @Operation(summary = "获取科目树形结构")
    public List<AccountSubjectDTO> getSubjectTree() {
        return financeService.getSubjectTree();
    }

    @GetMapping("/subjects/detail")
    @Operation(summary = "获取所有明细科目")
    public List<AccountSubjectDTO> getDetailSubjects() {
        return financeService.getDetailSubjects();
    }

    @GetMapping("/subjects/validate-code")
    @Operation(summary = "验证科目编码唯一性")
    public boolean validateSubjectCode(@RequestParam String subjectCode,
                                       @RequestParam(required = false) Long excludeId) {
        return financeService.validateSubjectCode(subjectCode, excludeId);
    }

    // ==================== 凭证管理 ====================

    @PostMapping("/vouchers")
    @Operation(summary = "创建凭证")
    public VoucherDTO createVoucher(@Valid @RequestBody VoucherSaveRequest request,
                                    @RequestParam Long operatorId) {
        return financeService.createVoucher(request, operatorId);
    }

    @PutMapping("/vouchers/{voucherId}")
    @Operation(summary = "更新凭证")
    public VoucherDTO updateVoucher(@PathVariable Long voucherId,
                                    @Valid @RequestBody VoucherSaveRequest request,
                                    @RequestParam Long operatorId) {
        return financeService.updateVoucher(voucherId, request, operatorId);
    }

    @DeleteMapping("/vouchers/{voucherId}")
    @Operation(summary = "删除凭证")
    public void deleteVoucher(@PathVariable Long voucherId,
                              @RequestParam Long operatorId) {
        financeService.deleteVoucher(voucherId, operatorId);
    }

    @GetMapping("/vouchers/{voucherId}")
    @Operation(summary = "获取凭证详情")
    public VoucherDTO getVoucherById(@PathVariable Long voucherId) {
        return financeService.getVoucherById(voucherId);
    }

    @PostMapping("/vouchers/list")
    @Operation(summary = "查询凭证列表")
    public IPage<VoucherDTO> queryVouchers(@RequestBody VoucherQueryRequest request) {
        return financeService.queryVouchers(request);
    }

    @PostMapping("/vouchers/{voucherId}/review")
    @Operation(summary = "审核凭证")
    public void reviewVoucher(@PathVariable Long voucherId,
                              @RequestParam Long operatorId) {
        financeService.reviewVoucher(voucherId, operatorId);
    }

    @PostMapping("/vouchers/{voucherId}/cancel-review")
    @Operation(summary = "取消审核")
    public void cancelReview(@PathVariable Long voucherId,
                             @RequestParam Long operatorId) {
        financeService.cancelReview(voucherId, operatorId);
    }

    @PostMapping("/vouchers/{voucherId}/bookkeeping")
    @Operation(summary = "记账")
    public void bookkeeping(@PathVariable Long voucherId,
                            @RequestParam Long operatorId) {
        financeService.bookkeeping(voucherId, operatorId);
    }

    @PostMapping("/vouchers/{voucherId}/cancel-bookkeeping")
    @Operation(summary = "取消记账")
    public void cancelBookkeeping(@PathVariable Long voucherId,
                                  @RequestParam Long operatorId) {
        financeService.cancelBookkeeping(voucherId, operatorId);
    }

    @PostMapping("/vouchers/{voucherId}/cancel")
    @Operation(summary = "作废凭证")
    public void cancelVoucher(@PathVariable Long voucherId,
                              @RequestParam Long operatorId) {
        financeService.cancelVoucher(voucherId, operatorId);
    }

    // ==================== 账簿管理 ====================

    @PostMapping("/ledger/detail")
    @Operation(summary = "查询明细账")
    public IPage<LedgerDTO> queryDetailLedger(@RequestBody LedgerQueryRequest request) {
        return financeService.queryDetailLedger(request);
    }

    @PostMapping("/ledger/general")
    @Operation(summary = "查询总账")
    public IPage<LedgerDTO> queryGeneralLedger(@RequestBody LedgerQueryRequest request) {
        return financeService.queryGeneralLedger(request);
    }

    @GetMapping("/subjects/{subjectId}/balance")
    @Operation(summary = "获取科目余额")
    public BigDecimal getSubjectBalance(@PathVariable Long subjectId,
                                        @RequestParam String accountingPeriod) {
        return financeService.getSubjectBalance(subjectId, accountingPeriod);
    }

    // ==================== 财务报表 ====================

    @GetMapping("/reports/balance-sheet")
    @Operation(summary = "生成资产负债表")
    public Map<String, Object> generateBalanceSheet(@RequestParam String accountingPeriod) {
        return financeService.generateBalanceSheet(accountingPeriod);
    }

    @GetMapping("/reports/income-statement")
    @Operation(summary = "生成利润表")
    public Map<String, Object> generateIncomeStatement(@RequestParam String startPeriod,
                                                       @RequestParam String endPeriod) {
        return financeService.generateIncomeStatement(startPeriod, endPeriod);
    }

    @GetMapping("/reports/cash-flow")
    @Operation(summary = "生成现金流量表")
    public Map<String, Object> generateCashFlowStatement(@RequestParam String startPeriod,
                                                         @RequestParam String endPeriod) {
        return financeService.generateCashFlowStatement(startPeriod, endPeriod);
    }
}

