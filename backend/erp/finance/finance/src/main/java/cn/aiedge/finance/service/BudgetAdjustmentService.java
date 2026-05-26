package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.BudgetAdjustment;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BudgetAdjustmentService extends IService<BudgetAdjustment> {
    
    List<BudgetAdjustment> listByBudgetId(Long tenantId, Long budgetId);
    
    List<BudgetAdjustment> listByStatus(Long tenantId, Integer status);
    
    Page<BudgetAdjustment> pageList(Long tenantId, Long budgetId, Integer status, Page<BudgetAdjustment> page);
    
    boolean createAdjustment(BudgetAdjustment adjustment);
    
    boolean submitForApproval(Long tenantId, Long adjustmentId);
    
    boolean approve(Long tenantId, Long adjustmentId);
    
    boolean reject(Long tenantId, Long adjustmentId, String reason);
    
    String generateAdjustmentNo(Long tenantId);
}