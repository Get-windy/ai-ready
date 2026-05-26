package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.BudgetItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface BudgetItemService extends IService<BudgetItem> {
    
    List<BudgetItem> listByBudgetId(Long tenantId, Long budgetId);
    
    List<BudgetItem> listAlertItems(Long tenantId);
    
    Page<BudgetItem> pageList(Long tenantId, Long budgetId, Long subjectId, Integer alertFlag, Page<BudgetItem> page);
    
    boolean createItem(BudgetItem item);
    
    boolean updateItem(BudgetItem item);
    
    boolean deleteItem(Long tenantId, Long itemId);
    
    boolean updateUsedAmount(Long tenantId, Long itemId, BigDecimal amount);
    
    boolean checkAlert(Long tenantId, Long itemId);
    
    BigDecimal sumBudgetAmount(Long tenantId, Long budgetId);
    
    BigDecimal sumUsedAmount(Long tenantId, Long budgetId);
}