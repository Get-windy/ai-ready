package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.erp.finance.mapper.FinanceAccountMapper;
import cn.aiedge.erp.finance.model.entity.FinanceAccount;
import cn.aiedge.erp.finance.service.FinanceAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 财务账户Service实现类
 */
@Service
public class FinanceAccountServiceImpl implements FinanceAccountService {

    @Autowired
    private FinanceAccountMapper financeAccountMapper;

    @Override
    public List<FinanceAccount> listAccounts(Integer status, String accountType) {
        LambdaQueryWrapper<FinanceAccount> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(FinanceAccount::getStatus, status);
        }
        if (accountType != null) {
            wrapper.eq(FinanceAccount::getAccountType, Integer.valueOf(accountType));
        }
        return financeAccountMapper.selectList(wrapper);
    }

    @Override
    public Object getAccountStatistics() {
        Long total = financeAccountMapper.selectCount(null);
        long enabledCount = financeAccountMapper.selectList(
                new LambdaQueryWrapper<FinanceAccount>().eq(FinanceAccount::getStatus, 1)).size();
        Long enabled = enabledCount;

        BigDecimal totalBalance = financeAccountMapper.selectList(null).stream()
                .map(FinanceAccount::getBalance)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "total", total,
                "enabled", enabled,
                "disabled", total - enabled,
                "totalBalance", totalBalance
        );
    }

    @Override
    public boolean updateAccountStatus(Long id, Integer status) {
        FinanceAccount account = financeAccountMapper.selectById(id);
        if (account == null) {
            return false;
        }
        account.setStatus(status);
        financeAccountMapper.updateById(account);
        return true;
    }

    @Override
    public boolean updateAccountBalance(Long accountId, BigDecimal amount) {
        FinanceAccount account = financeAccountMapper.selectById(accountId);
        if (account == null) {
            return false;
        }
        account.setBalance(account.getBalance() != null ? account.getBalance().add(amount) : amount);
        financeAccountMapper.updateById(account);
        return true;
    }
}
