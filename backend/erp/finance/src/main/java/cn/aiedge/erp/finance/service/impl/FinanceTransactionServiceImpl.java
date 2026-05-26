package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.erp.finance.model.entity.FinanceTransaction;
import cn.aiedge.erp.finance.repository.FinanceTransactionRepository;
import cn.aiedge.erp.finance.service.FinanceTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 财务交易Service实现类
 */
@Service
public class FinanceTransactionServiceImpl implements FinanceTransactionService {
    
    @Autowired
    private FinanceTransactionRepository financeTransactionRepository;
    
    @Override
    public boolean createTransaction(FinanceTransaction transaction) {
        try {
            financeTransactionRepository.save(transaction);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public List<FinanceTransaction> listTransactions(Integer transactionType, Integer status, 
            LocalDateTime startDate, LocalDateTime endDate) {
        if (transactionType != null) {
            return financeTransactionRepository.findByTransactionType(transactionType);
        } else if (status != null) {
            return financeTransactionRepository.findByStatus(status);
        } else if (startDate != null && endDate != null) {
            return financeTransactionRepository.findByTransactionTimeBetween(startDate, endDate);
        }
        return financeTransactionRepository.findAll();
    }
    
    @Override
    public Object getTransactionStatistics(Integer bizType, String period) {
        // 统计交易数据
        Long total = financeTransactionRepository.count();
        List<FinanceTransaction> transactions = financeTransactionRepository.findAll();
        
        Double income = transactions.stream()
            .filter(t -> t.getTransactionType() == 1)
            .mapToDouble(t -> t.getAmount().doubleValue())
            .sum();
            
        Double expense = transactions.stream()
            .filter(t -> t.getTransactionType() == 2)
            .mapToDouble(t -> t.getAmount().doubleValue())
            .sum();
        
        return Map.of(
            "total", total,
            "income", income,
            "expense", expense,
            "net", income - expense
        );
    }
    
    @Override
    public boolean approveTransaction(Long id, String approvedBy) {
        FinanceTransaction transaction = financeTransactionRepository.findById(id).orElse(null);
        if (transaction == null) {
            return false;
        }
        transaction.setStatus(1);
        transaction.setApprovedBy(approvedBy);
        transaction.setApprovedAt(LocalDateTime.now());
        financeTransactionRepository.save(transaction);
        return true;
    }
    
    @Override
    public boolean revokeTransaction(Long id) {
        FinanceTransaction transaction = financeTransactionRepository.findById(id).orElse(null);
        if (transaction == null) {
            return false;
        }
        transaction.setStatus(2);
        financeTransactionRepository.save(transaction);
        return true;
    }
    
    @Override
    public BigDecimal getAccountBalance(Long accountId) {
        // 计算账户余额的逻辑
        return BigDecimal.ZERO;
    }
}
