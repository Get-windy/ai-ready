package cn.aiedge.erp.finance.controller;

import cn.aiedge.erp.finance.model.entity.FinanceTransaction;
import cn.aiedge.erp.finance.service.FinanceTransactionService;
import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 财务交易Controller
 */
@Tag(name = "财务交易管理", description = "财务交易管理接口")
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/transaction")
public class FinanceTransactionController {
    
    @Autowired
    private FinanceTransactionService financeTransactionService;
    
    /**
     * 创建交易记录
     */
    @OperationLog(module = "财务交易管理", type = "CREATE", desc = "创建交易记录")
    @PreAuthorize("hasPermission('/api/erp/finance/transaction', 'finance:transaction:create')")
    @PostMapping("/")
    public Result<FinanceTransaction> createTransaction(@RequestBody FinanceTransaction transaction) {
        boolean success = financeTransactionService.createTransaction(transaction);
        return success ? Result.success(transaction) : Result.error("创建失败");
    }
    
    /**
     * 查询交易列表
     */
    @OperationLog(module = "财务交易管理", type = "QUERY", desc = "查询交易列表")
    @PreAuthorize("hasPermission('/api/erp/finance/transaction/list', 'finance:transaction:view')")
    @GetMapping("/list")
    public Result<List<FinanceTransaction>> listTransactions(
            @Parameter(description = "交易类型") @RequestParam(required = false) Integer transactionType,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "开始日期") @RequestParam(required = false) LocalDateTime startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) LocalDateTime endDate) {
        List<FinanceTransaction> transactions = financeTransactionService.listTransactions(transactionType, status, startDate, endDate);
        return Result.success(transactions);
    }
    
    /**
     * 查询交易统计
     */
    @OperationLog(module = "财务交易管理", type = "QUERY", desc = "查询交易统计")
    @PreAuthorize("hasPermission('/api/erp/finance/transaction/statistics', 'finance:transaction:view')")
    @GetMapping("/statistics")
    public Result<Object> getTransactionStatistics(
            @Parameter(description = "业务类型") @RequestParam(required = false) Integer bizType,
            @Parameter(description = "期间") @RequestParam(required = false) String period) {
        Object statistics = financeTransactionService.getTransactionStatistics(bizType, period);
        return Result.success(statistics);
    }
    
    /**
     * 审核交易
     */
    @OperationLog(module = "财务交易管理", type = "UPDATE", desc = "审核交易")
    @PreAuthorize("hasPermission('/api/erp/finance/transaction/approve', 'finance:transaction:approve')")
    @PutMapping("/approve/{id}")
    public Result<Void> approveTransaction(
            @Parameter(description = "交易ID") @PathVariable Long id,
            @Parameter(description = "审核人") @RequestHeader("userId") String approvedBy) {
        boolean success = financeTransactionService.approveTransaction(id, approvedBy);
        return success ? Result.success() : Result.error("审核失败");
    }
    
    /**
     * 撤销交易
     */
    @OperationLog(module = "财务交易管理", type = "DELETE", desc = "撤销交易")
    @PreAuthorize("hasPermission('/api/erp/finance/transaction/revoke', 'finance:transaction:revoke')")
    @DeleteMapping("/revoke/{id}")
    public Result<Void> revokeTransaction(@PathVariable Long id) {
        boolean success = financeTransactionService.revokeTransaction(id);
        return success ? Result.success() : Result.error("撤销失败");
    }
}
