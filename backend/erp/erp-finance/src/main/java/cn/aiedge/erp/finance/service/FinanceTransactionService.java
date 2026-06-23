package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.model.entity.FinanceTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 财务交易Service接口
 */
public interface FinanceTransactionService {
    
    /**
     * 创建交易记录
     */
    boolean createTransaction(FinanceTransaction transaction);
    
    /**
     * 查询交易列表
     */
    List<FinanceTransaction> listTransactions(Integer transactionType, Integer status, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 查询交易统计
     */
    Object getTransactionStatistics(Integer bizType, String period);
    
    /**
     * 审核交易
     */
    boolean approveTransaction(Long id, String approvedBy);
    
    /**
     * 撤销交易
     */
    boolean revokeTransaction(Long id);
    
    /**
     * 查询账户余额
     */
    BigDecimal getAccountBalance(Long accountId);
}
