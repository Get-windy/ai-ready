package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.AccountPeriod;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AccountPeriodService extends IService<AccountPeriod> {
    
    List<AccountPeriod> listAll(Long tenantId);
    
    List<AccountPeriod> listByYear(Long tenantId, Integer year);
    
    AccountPeriod getByYearMonth(Long tenantId, Integer year, Integer month);
    
    AccountPeriod getCurrentPeriod(Long tenantId);
    
    Page<AccountPeriod> pageList(Long tenantId, Integer year, Integer status, Page<AccountPeriod> page);
    
    boolean createPeriod(AccountPeriod period);
    
    boolean updatePeriod(AccountPeriod period);
    
    boolean deletePeriod(Long tenantId, Long periodId);
    
    boolean openPeriod(Long tenantId, Integer year, Integer month);
    
    boolean closePeriod(Long tenantId, Integer year, Integer month);
    
    boolean setCurrentPeriod(Long tenantId, Long periodId);
    
    boolean initYearPeriods(Long tenantId, Integer year);
    
    boolean yearEndClose(Long tenantId, Integer year);
    
    boolean checkPeriodCanClose(Long tenantId, Integer year, Integer month);
}