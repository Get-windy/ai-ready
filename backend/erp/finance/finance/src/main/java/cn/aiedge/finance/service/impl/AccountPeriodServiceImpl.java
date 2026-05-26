package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.AccountBalance;
import cn.aiedge.finance.entity.AccountPeriod;
import cn.aiedge.finance.entity.AccountSubject;
import cn.aiedge.finance.mapper.AccountBalanceMapper;
import cn.aiedge.finance.mapper.AccountPeriodMapper;
import cn.aiedge.finance.mapper.AccountSubjectMapper;
import cn.aiedge.finance.service.AccountBalanceService;
import cn.aiedge.finance.service.AccountPeriodService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AccountPeriodServiceImpl extends ServiceImpl<AccountPeriodMapper, AccountPeriod> implements AccountPeriodService {
    
    @Autowired
    private AccountBalanceService accountBalanceService;
    
    @Autowired
    private AccountSubjectMapper accountSubjectMapper;
    
    @Autowired
    private AccountBalanceMapper accountBalanceMapper;
    
    @Override
    public List<AccountPeriod> listAll(Long tenantId) {
        return baseMapper.listAll(tenantId);
    }
    
    @Override
    public List<AccountPeriod> listByYear(Long tenantId, Integer year) {
        return baseMapper.listByYear(tenantId, year);
    }
    
    @Override
    public AccountPeriod getByYearMonth(Long tenantId, Integer year, Integer month) {
        return baseMapper.getByYearMonth(tenantId, year, month);
    }
    
    @Override
    public AccountPeriod getCurrentPeriod(Long tenantId) {
        return baseMapper.getCurrentPeriod(tenantId);
    }
    
    @Override
    public Page<AccountPeriod> pageList(Long tenantId, Integer year, Integer status, Page<AccountPeriod> page) {
        LambdaQueryWrapper<AccountPeriod> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountPeriod::getTenantId, tenantId)
               .eq(AccountPeriod::getDeleted, 0);
        if (year != null) {
            wrapper.eq(AccountPeriod::getYear, year);
        }
        if (status != null) {
            wrapper.eq(AccountPeriod::getStatus, status);
        }
        wrapper.orderByDesc(AccountPeriod::getYear).orderByDesc(AccountPeriod::getMonth);
        return this.page(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createPeriod(AccountPeriod period) {
        AccountPeriod existing = this.getByYearMonth(period.getTenantId(), period.getYear(), period.getMonth());
        if (existing != null) {
            throw new RuntimeException("该会计期间已存在");
        }
        period.setPeriodCode(String.format("%04d%02d", period.getYear(), period.getMonth()));
        period.setStartDate(LocalDate.of(period.getYear(), period.getMonth(), 1));
        period.setEndDate(LocalDate.of(period.getYear(), period.getMonth(), 1).plusMonths(1).minusDays(1));
        period.setStatus(0);
        period.setIsCurrent(0);
        return this.save(period);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePeriod(AccountPeriod period) {
        return this.updateById(period);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePeriod(Long tenantId, Long periodId) {
        AccountPeriod period = this.getById(periodId);
        if (period == null) {
            throw new RuntimeException("会计期间不存在");
        }
        if (period.getStatus() == 2) {
            throw new RuntimeException("已结账期间不能删除");
        }
        return this.removeById(periodId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean openPeriod(Long tenantId, Integer year, Integer month) {
        AccountPeriod period = this.getByYearMonth(tenantId, year, month);
        if (period == null) {
            throw new RuntimeException("会计期间不存在");
        }
        if (period.getStatus() != 0) {
            throw new RuntimeException("只有未开账期间才能开账");
        }
        period.setStatus(1);
        this.updateById(period);
        String periodStr = String.format("%04d%02d", year, month);
        accountBalanceService.initializePeriodBalance(tenantId, periodStr);
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closePeriod(Long tenantId, Integer year, Integer month) {
        AccountPeriod period = this.getByYearMonth(tenantId, year, month);
        if (period == null) {
            throw new RuntimeException("会计期间不存在");
        }
        if (period.getStatus() != 1) {
            throw new RuntimeException("只有已开账期间才能结账");
        }
        if (!checkPeriodCanClose(tenantId, year, month)) {
            throw new RuntimeException("存在未审核凭证，不能结账");
        }
        period.setStatus(2);
        period.setClosedDate(LocalDate.now());
        return this.updateById(period);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setCurrentPeriod(Long tenantId, Long periodId) {
        AccountPeriod period = this.getById(periodId);
        if (period == null) {
            throw new RuntimeException("会计期间不存在");
        }
        if (period.getStatus() != 1) {
            throw new RuntimeException("只有已开账期间才能设为当前期间");
        }
        baseMapper.clearCurrentFlag(tenantId);
        baseMapper.setCurrentPeriod(periodId);
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean initYearPeriods(Long tenantId, Integer year) {
        for (int month = 1; month <= 12; month++) {
            AccountPeriod period = new AccountPeriod();
            period.setTenantId(tenantId);
            period.setYear(year);
            period.setMonth(month);
            this.createPeriod(period);
        }
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean yearEndClose(Long tenantId, Integer year) {
        AccountPeriod lastPeriod = this.getByYearMonth(tenantId, year, 12);
        if (lastPeriod == null || lastPeriod.getStatus() != 2) {
            throw new RuntimeException("12月份未结账，不能进行年终结账");
        }
        String currentPeriodStr = String.format("%04d12", year);
        String nextPeriodStr = String.format("%04d01", year + 1);
        accountBalanceService.carryOverBalance(tenantId, currentPeriodStr, nextPeriodStr);
        AccountPeriod firstPeriodNextYear = this.getByYearMonth(tenantId, year + 1, 1);
        if (firstPeriodNextYear != null) {
            firstPeriodNextYear.setStatus(1);
            this.updateById(firstPeriodNextYear);
        }
        return true;
    }
    
    @Override
    public boolean checkPeriodCanClose(Long tenantId, Integer year, Integer month) {
        return true;
    }
}