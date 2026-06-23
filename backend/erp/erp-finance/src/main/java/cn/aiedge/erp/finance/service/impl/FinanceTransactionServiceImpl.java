package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.erp.finance.mapper.FinanceTransactionMapper;
import cn.aiedge.erp.finance.model.entity.FinanceTransaction;
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
    private FinanceTransactionMapper financeTransactionMapper;

    @Override
    public boolean createTransaction(FinanceTransaction transaction) {
        try {
            financeTransactionMapper.insert(transaction);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<FinanceTransaction> listTransactions(Integer transactionType, Integer status,
            LocalDateTime startDate, LocalDateTime endDate) {
        if (transactionType != null) {
            return financeTransactionMapper.findByTransactionType(transactionType);
        } else if (status != null) {
            return financeTransactionMapper.findByStatus(status);
        } else if (startDate != null && endDate != null) {
            return financeTransactionMapper.findByTransactionTimeBetween(startDate, endDate);
        }
        return financeTransactionMapper.selectList(null);
    }

    @Override
    public Object getTransactionStatistics(Integer bizType, String period) {
        Long total = financeTransactionMapper.selectCount(null);
        List<FinanceTransaction> transactions = financeTransactionMapper.selectList(null);

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
        FinanceTransaction transaction = financeTransactionMapper.selectById(id);
        if (transaction == null) {
            return false;
        }
        transaction.setStatus(1);
        transaction.setApprovedBy(approvedBy);
        transaction.setApprovedAt(LocalDateTime.now());
        financeTransactionMapper.updateById(transaction);
        return true;
    }

    @Override
    public boolean revokeTransaction(Long id) {
        FinanceTransaction transaction = financeTransactionMapper.selectById(id);
        if (transaction == null) {
            return false;
        }
        transaction.setStatus(2);
        financeTransactionMapper.updateById(transaction);
        return true;
    }

    @Override
    public BigDecimal getAccountBalance(Long accountId) {
        return BigDecimal.ZERO;
    }
}
