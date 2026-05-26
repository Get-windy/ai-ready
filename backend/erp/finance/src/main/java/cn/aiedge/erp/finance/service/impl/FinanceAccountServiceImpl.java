package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.erp.finance.model.entity.FinanceAccount;
import cn.aiedge.erp.finance.repository.FinanceAccountRepository;
import cn.aiedge.erp.finance.service.FinanceAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 财务账户Service实现类
 */
@Service
public class FinanceAccountServiceImpl implements FinanceAccountService {
    
    @Autowired
    private FinanceAccountRepository financeAccountRepository;
    
    @Override
    public List<FinanceAccount> listAccounts(Integer status, String accountType) {
        FinanceAccount example = new FinanceAccount();
        if (status != null) {
            example.setStatus(status);
        }
        if (accountType != null) {
            example.setAccountType(Integer.valueOf(accountType));
        }
        
        Example<FinanceAccount> exampleMatcher = Example.of(example, 
            ExampleMatcher.matching()
                .withIgnoreNullValues()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING));
        
        return financeAccountRepository.findAll(exampleMatcher);
    }
    
    @Override
    public Object getAccountStatistics() {
        // 统计账户总数
        Long total = financeAccountRepository.count();
        
        // 统计启用账户数
        long enabledCount = financeAccountRepository.findByStatus(1).size();
        Long enabled = enabledCount;
        
        // 统计账户余额总计
        Double totalBalance = financeAccountRepository.findAll().stream()
            .mapToDouble(FinanceAccount::getBalance).sum();
        
        return Map.of(
            "total", total,
            "enabled", enabled,
            "disabled", total - enabled,
            "totalBalance", totalBalance
        );
    }
    
    @Override
    public boolean updateAccountStatus(Long id, Integer status) {
        FinanceAccount account = financeAccountRepository.findById(id).orElse(null);
        if (account == null) {
            return false;
        }
        account.setStatus(status);
        financeAccountRepository.save(account);
        return true;
    }
    
    @Override
    public boolean updateAccountBalance(Long accountId, Double amount) {
        FinanceAccount account = financeAccountRepository.findById(accountId).orElse(null);
        if (account == null) {
            return false;
        }
        account.setBalance(account.getBalance() + amount);
        financeAccountRepository.save(account);
        return true;
    }
}
