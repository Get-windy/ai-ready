package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.AccountBalance;
import cn.aiedge.finance.entity.AccountSubject;
import cn.aiedge.finance.mapper.AccountBalanceMapper;
import cn.aiedge.finance.mapper.AccountSubjectMapper;
import cn.aiedge.finance.service.AccountBalanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccountBalanceServiceImpl extends ServiceImpl<AccountBalanceMapper, AccountBalance> implements AccountBalanceService {
    
    @Autowired
    private AccountSubjectMapper accountSubjectMapper;
    
    @Override
    public List<AccountBalance> listByPeriod(Long tenantId, String period) {
        return baseMapper.listByPeriod(tenantId, period);
    }
    
    @Override
    public AccountBalance getBySubjectAndPeriod(Long tenantId, Long subjectId, String period) {
        return baseMapper.getBySubjectAndPeriod(tenantId, subjectId, period);
    }
    
    @Override
    public AccountBalance getByCodeAndPeriod(Long tenantId, String subjectCode, String period) {
        return baseMapper.getByCodeAndPeriod(tenantId, subjectCode, period);
    }
    
    @Override
    public Page<AccountBalance> pageList(Long tenantId, String period, String subjectCode, Integer subjectType, Page<AccountBalance> page) {
        LambdaQueryWrapper<AccountBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountBalance::getTenantId, tenantId)
               .eq(AccountBalance::getDeleted, 0)
               .eq(AccountBalance::getPeriod, period);
        if (subjectCode != null && !subjectCode.isEmpty()) {
            wrapper.like(AccountBalance::getSubjectCode, subjectCode);
        }
        wrapper.orderByAsc(AccountBalance::getSubjectCode);
        return this.page(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBalance(Long tenantId, Long subjectId, String period, BigDecimal debitAmount, BigDecimal creditAmount) {
        AccountBalance balance = this.getBySubjectAndPeriod(tenantId, subjectId, period);
        if (balance == null) {
            AccountSubject subject = accountSubjectMapper.selectById(subjectId);
            if (subject == null) {
                throw new RuntimeException("科目不存在");
            }
            balance = new AccountBalance();
            balance.setSubjectId(subjectId);
            balance.setSubjectCode(subject.getSubjectCode());
            balance.setPeriod(period);
            balance.setTenantId(tenantId);
            balance.setInitialDebit(BigDecimal.ZERO);
            balance.setInitialCredit(BigDecimal.ZERO);
            balance.setPeriodDebit(BigDecimal.ZERO);
            balance.setPeriodCredit(BigDecimal.ZERO);
            balance.setYearDebit(BigDecimal.ZERO);
            balance.setYearCredit(BigDecimal.ZERO);
            balance.setEndingDebit(BigDecimal.ZERO);
            balance.setEndingCredit(BigDecimal.ZERO);
            this.save(balance);
        }
        BigDecimal periodDebit = balance.getPeriodDebit().add(debitAmount != null ? debitAmount : BigDecimal.ZERO);
        BigDecimal periodCredit = balance.getPeriodCredit().add(creditAmount != null ? creditAmount : BigDecimal.ZERO);
        balance.setPeriodDebit(periodDebit);
        balance.setPeriodCredit(periodCredit);
        balance.setYearDebit(balance.getYearDebit().add(debitAmount != null ? debitAmount : BigDecimal.ZERO));
        balance.setYearCredit(balance.getYearCredit().add(creditAmount != null ? creditAmount : BigDecimal.ZERO));
        AccountSubject subject = accountSubjectMapper.selectById(subjectId);
        if (subject != null && subject.getBalanceDirection() == 1) {
            balance.setEndingDebit(balance.getInitialDebit().add(balance.getYearDebit()));
            balance.setEndingCredit(balance.getInitialCredit().add(balance.getYearCredit()));
        } else {
            balance.setEndingDebit(balance.getInitialDebit().add(balance.getYearDebit()));
            balance.setEndingCredit(balance.getInitialCredit().add(balance.getYearCredit()));
        }
        return this.updateById(balance);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean initializePeriodBalance(Long tenantId, String period) {
        List<AccountSubject> subjects = accountSubjectMapper.listLeafSubjects(tenantId);
        for (AccountSubject subject : subjects) {
            AccountBalance balance = new AccountBalance();
            balance.setSubjectId(subject.getId());
            balance.setSubjectCode(subject.getSubjectCode());
            balance.setPeriod(period);
            balance.setTenantId(tenantId);
            balance.setInitialDebit(BigDecimal.ZERO);
            balance.setInitialCredit(BigDecimal.ZERO);
            balance.setPeriodDebit(BigDecimal.ZERO);
            balance.setPeriodCredit(BigDecimal.ZERO);
            balance.setYearDebit(BigDecimal.ZERO);
            balance.setYearCredit(BigDecimal.ZERO);
            balance.setEndingDebit(BigDecimal.ZERO);
            balance.setEndingCredit(BigDecimal.ZERO);
            this.save(balance);
        }
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean carryOverBalance(Long tenantId, String fromPeriod, String toPeriod) {
        List<AccountBalance> fromBalances = this.listByPeriod(tenantId, fromPeriod);
        for (AccountBalance fromBalance : fromBalances) {
            AccountBalance toBalance = new AccountBalance();
            toBalance.setSubjectId(fromBalance.getSubjectId());
            toBalance.setSubjectCode(fromBalance.getSubjectCode());
            toBalance.setPeriod(toPeriod);
            toBalance.setTenantId(tenantId);
            toBalance.setInitialDebit(fromBalance.getEndingDebit());
            toBalance.setInitialCredit(fromBalance.getEndingCredit());
            toBalance.setPeriodDebit(BigDecimal.ZERO);
            toBalance.setPeriodCredit(BigDecimal.ZERO);
            toBalance.setYearDebit(BigDecimal.ZERO);
            toBalance.setYearCredit(BigDecimal.ZERO);
            toBalance.setEndingDebit(fromBalance.getEndingDebit());
            toBalance.setEndingCredit(fromBalance.getEndingCredit());
            this.save(toBalance);
        }
        return true;
    }
    
    @Override
    public BigDecimal calculateTotalAssets(Long tenantId, String period) {
        BigDecimal result = baseMapper.calculateTotalAssets(tenantId, period);
        return result != null ? result : BigDecimal.ZERO;
    }
    
    @Override
    public BigDecimal calculateTotalLiabilitiesAndEquity(Long tenantId, String period) {
        BigDecimal result = baseMapper.calculateTotalLiabilitiesAndEquity(tenantId, period);
        return result != null ? result : BigDecimal.ZERO;
    }
    
    @Override
    public Map<String, BigDecimal> getTrialBalance(Long tenantId, String period) {
        List<AccountBalance> balances = this.listByPeriod(tenantId, period);
        BigDecimal totalInitialDebit = BigDecimal.ZERO;
        BigDecimal totalInitialCredit = BigDecimal.ZERO;
        BigDecimal totalPeriodDebit = BigDecimal.ZERO;
        BigDecimal totalPeriodCredit = BigDecimal.ZERO;
        BigDecimal totalEndingDebit = BigDecimal.ZERO;
        BigDecimal totalEndingCredit = BigDecimal.ZERO;
        for (AccountBalance balance : balances) {
            totalInitialDebit = totalInitialDebit.add(balance.getInitialDebit());
            totalInitialCredit = totalInitialCredit.add(balance.getInitialCredit());
            totalPeriodDebit = totalPeriodDebit.add(balance.getPeriodDebit());
            totalPeriodCredit = totalPeriodCredit.add(balance.getPeriodCredit());
            totalEndingDebit = totalEndingDebit.add(balance.getEndingDebit());
            totalEndingCredit = totalEndingCredit.add(balance.getEndingCredit());
        }
        Map<String, BigDecimal> result = new HashMap<>();
        result.put("totalInitialDebit", totalInitialDebit);
        result.put("totalInitialCredit", totalInitialCredit);
        result.put("totalPeriodDebit", totalPeriodDebit);
        result.put("totalPeriodCredit", totalPeriodCredit);
        result.put("totalEndingDebit", totalEndingDebit);
        result.put("totalEndingCredit", totalEndingCredit);
        return result;
    }
    
    @Override
    public Map<String, Object> getBalanceSheet(Long tenantId, String period) {
        List<AccountBalance> balances = this.listByPeriod(tenantId, period);
        Map<String, Object> result = new HashMap<>();
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalEquity = BigDecimal.ZERO;
        for (AccountBalance balance : balances) {
            AccountSubject subject = accountSubjectMapper.selectById(balance.getSubjectId());
            if (subject == null) continue;
            BigDecimal netBalance = balance.getEndingDebit().subtract(balance.getEndingCredit());
            if (subject.getSubjectType() == 1 || subject.getSubjectType() == 4) {
                if (netBalance.compareTo(BigDecimal.ZERO) > 0) {
                    totalAssets = totalAssets.add(netBalance);
                }
                result.put(subject.getSubjectCode(), netBalance);
            } else if (subject.getSubjectType() == 2) {
                if (netBalance.compareTo(BigDecimal.ZERO) < 0) {
                    totalLiabilities = totalLiabilities.add(netBalance.abs());
                }
                result.put(subject.getSubjectCode(), netBalance.abs());
            } else if (subject.getSubjectType() == 3) {
                if (netBalance.compareTo(BigDecimal.ZERO) < 0) {
                    totalEquity = totalEquity.add(netBalance.abs());
                }
                result.put(subject.getSubjectCode(), netBalance.abs());
            }
        }
        result.put("totalAssets", totalAssets);
        result.put("totalLiabilities", totalLiabilities);
        result.put("totalEquity", totalEquity);
        result.put("totalLiabilitiesAndEquity", totalLiabilities.add(totalEquity));
        return result;
    }
    
    @Override
    public Map<String, Object> getIncomeStatement(Long tenantId, String period) {
        List<AccountBalance> balances = this.listByPeriod(tenantId, period);
        Map<String, Object> result = new HashMap<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        for (AccountBalance balance : balances) {
            AccountSubject subject = accountSubjectMapper.selectById(balance.getSubjectId());
            if (subject == null) continue;
            if (subject.getSubjectType() != 5) continue;
            String codePrefix = subject.getSubjectCode().substring(0, 2);
            BigDecimal amount = balance.getPeriodCredit().subtract(balance.getPeriodDebit());
            if ("50".equals(codePrefix) || "51".equals(codePrefix)) {
                totalRevenue = totalRevenue.add(amount);
                result.put(subject.getSubjectCode(), amount);
            } else if ("53".equals(codePrefix) || "54".equals(codePrefix) || "55".equals(codePrefix) || "56".equals(codePrefix) || "57".equals(codePrefix)) {
                totalCost = totalCost.add(amount.abs());
                result.put(subject.getSubjectCode(), amount.abs());
            }
        }
        result.put("totalRevenue", totalRevenue);
        result.put("totalCost", totalCost);
        result.put("netProfit", totalRevenue.subtract(totalCost));
        return result;
    }
}