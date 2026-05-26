package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.AccountBalance;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface AccountBalanceService extends IService<AccountBalance> {
    
    List<AccountBalance> listByPeriod(Long tenantId, String period);
    
    AccountBalance getBySubjectAndPeriod(Long tenantId, Long subjectId, String period);
    
    AccountBalance getByCodeAndPeriod(Long tenantId, String subjectCode, String period);
    
    Page<AccountBalance> pageList(Long tenantId, String period, String subjectCode, Integer subjectType, Page<AccountBalance> page);
    
    boolean updateBalance(Long tenantId, Long subjectId, String period, BigDecimal debitAmount, BigDecimal creditAmount);
    
    boolean initializePeriodBalance(Long tenantId, String period);
    
    boolean carryOverBalance(Long tenantId, String fromPeriod, String toPeriod);
    
    BigDecimal calculateTotalAssets(Long tenantId, String period);
    
    BigDecimal calculateTotalLiabilitiesAndEquity(Long tenantId, String period);
    
    Map<String, BigDecimal> getTrialBalance(Long tenantId, String period);
    
    Map<String, Object> getBalanceSheet(Long tenantId, String period);
    
    Map<String, Object> getIncomeStatement(Long tenantId, String period);
}