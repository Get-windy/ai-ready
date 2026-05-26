package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.Budget;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface BudgetService extends IService<Budget> {
    
    List<Budget> listByStatus(Long tenantId, Integer status);
    
    List<Budget> listByPeriod(Long tenantId, String period);
    
    List<Budget> listByType(Long tenantId, Integer budgetType);
    
    Budget getByCode(Long tenantId, String budgetCode);
    
    Page<Budget> pageList(Long tenantId, String budgetCode, String budgetName, Integer budgetType, String period, Integer status, Page<Budget> page);
    
    Budget getDetail(Long tenantId, Long budgetId);
    
    boolean createBudget(Budget budget);
    
    boolean updateBudget(Budget budget);
    
    boolean deleteBudget(Long tenantId, Long budgetId);
    
    boolean submitForApproval(Long tenantId, Long budgetId);
    
    boolean approve(Long tenantId, Long budgetId);
    
    boolean reject(Long tenantId, Long budgetId, String reason);
    
    boolean startExecution(Long tenantId, Long budgetId);
    
    boolean close(Long tenantId, Long budgetId);
    
    boolean recordExecution(Long tenantId, Long budgetItemId, BigDecimal amount, String sourceType, Long sourceId, String sourceNo);
    
    boolean checkBudget(Long tenantId, Long budgetItemId, BigDecimal amount);
    
    Map<String, Object> getBudgetSummary(Long tenantId, String period);
    
    List<Budget> generateFromLastYear(Long tenantId, Integer year, BigDecimal adjustRate);
}