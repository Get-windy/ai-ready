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
    public Object getAccountStatistics(Integer status, Integer accountType, Integer accountLevel,
                                       String currency, String keyword) {
        LambdaQueryWrapper<FinanceAccount> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(FinanceAccount::getStatus, status);
        }
        if (accountType != null) {
            wrapper.eq(FinanceAccount::getAccountType, accountType);
        }
        if (accountLevel != null) {
            wrapper.eq(FinanceAccount::getAccountLevel, accountLevel);
        }
        if (currency != null && !currency.isBlank()) {
            wrapper.eq(FinanceAccount::getCurrency, currency.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(FinanceAccount::getAccountName, kw)
                    .or().like(FinanceAccount::getBankName, kw)
                    .or().like(FinanceAccount::getBankAccount, kw)
                    .or().like(FinanceAccount::getSubjectCode, kw));
        }

        List<FinanceAccount> rows = financeAccountMapper.selectList(wrapper);
        long total = rows.size();
        long enabled = rows.stream().filter(a -> a.getStatus() != null && a.getStatus() == 1).count();
        BigDecimal totalBalance = rows.stream()
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
        if (status == null || (status != 0 && status != 1)) {
            return false;
        }
        FinanceAccount account = financeAccountMapper.selectById(id);
        if (account == null) {
            return false;
        }
        account.setStatus(status);
        financeAccountMapper.updateById(account);
        return true;
    }

    /**
     * 余额调整（增量口径）：正数增加、负数减少（支付账户「余额调整」入口）。
     */
    @Override
    public boolean updateAccountBalance(Long accountId, BigDecimal amount) {
        if (amount == null) {
            return false;
        }
        FinanceAccount account = financeAccountMapper.selectById(accountId);
        if (account == null) {
            return false;
        }
        account.setBalance(account.getBalance() != null ? account.getBalance().add(amount) : amount);
        financeAccountMapper.updateById(account);
        return true;
    }
}
