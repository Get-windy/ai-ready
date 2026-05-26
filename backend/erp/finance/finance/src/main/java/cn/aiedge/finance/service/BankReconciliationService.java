package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.BankReconciliation;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface BankReconciliationService extends IService<BankReconciliation> {
    
    List<BankReconciliation> listByAccountId(Long tenantId, Long accountId);
    
    BankReconciliation getByAccountIdAndPeriod(Long tenantId, Long accountId, String period);
    
    Page<BankReconciliation> pageList(Long tenantId, Long accountId, Integer status, Page<BankReconciliation> page);
    
    BankReconciliation createReconciliation(Long tenantId, Long accountId, String period);
    
    boolean calculateReconciliation(Long tenantId, Long reconciliationId);
    
    boolean approve(Long tenantId, Long reconciliationId);
    
    boolean deleteReconciliation(Long tenantId, Long reconciliationId);
    
    Map<String, Object> getReconciliationDetail(Long tenantId, Long reconciliationId);
    
    List<Map<String, Object>> getUnmatchedItems(Long tenantId, Long reconciliationId);
    
    BigDecimal calculateAdjustedBalance(Long tenantId, Long accountId, String period);
}