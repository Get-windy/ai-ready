package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.Budget;
import cn.aiedge.finance.entity.BudgetExecution;
import cn.aiedge.finance.entity.BudgetItem;
import cn.aiedge.finance.enums.BudgetStatus;
import cn.aiedge.finance.mapper.BudgetExecutionMapper;
import cn.aiedge.finance.mapper.BudgetItemMapper;
import cn.aiedge.finance.mapper.BudgetMapper;
import cn.aiedge.finance.service.BudgetItemService;
import cn.aiedge.finance.service.BudgetService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BudgetServiceImpl extends ServiceImpl<BudgetMapper, Budget> implements BudgetService {
    
    @Autowired
    private BudgetItemMapper budgetItemMapper;
    
    @Autowired
    private BudgetItemService budgetItemService;
    
    @Autowired
    private BudgetExecutionMapper budgetExecutionMapper;
    
    @Override
    public List<Budget> listByStatus(Long tenantId, Integer status) {
        return baseMapper.listByStatus(tenantId, status);
    }
    
    @Override
    public List<Budget> listByPeriod(Long tenantId, String period) {
        return baseMapper.listByPeriod(tenantId, period);
    }
    
    @Override
    public List<Budget> listByType(Long tenantId, Integer budgetType) {
        return baseMapper.listByType(tenantId, budgetType);
    }
    
    @Override
    public Budget getByCode(Long tenantId, String budgetCode) {
        return baseMapper.getByCode(tenantId, budgetCode);
    }
    
    @Override
    public Page<Budget> pageList(Long tenantId, String budgetCode, String budgetName, Integer budgetType, String period, Integer status, Page<Budget> page) {
        LambdaQueryWrapper<Budget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Budget::getTenantId, tenantId)
               .eq(Budget::getDeleted, 0);
        if (budgetCode != null && !budgetCode.isEmpty()) {
            wrapper.like(Budget::getBudgetCode, budgetCode);
        }
        if (budgetName != null && !budgetName.isEmpty()) {
            wrapper.like(Budget::getBudgetName, budgetName);
        }
        if (budgetType != null) {
            wrapper.eq(Budget::getBudgetType, budgetType);
        }
        if (period != null && !period.isEmpty()) {
            wrapper.eq(Budget::getPeriod, period);
        }
        if (status != null) {
            wrapper.eq(Budget::getStatus, status);
        }
        wrapper.orderByDesc(Budget::getPeriod);
        return this.page(page, wrapper);
    }
    
    @Override
    public Budget getDetail(Long tenantId, Long budgetId) {
        Budget budget = this.getById(budgetId);
        if (budget != null) {
            List<BudgetItem> items = budgetItemMapper.listByBudgetId(tenantId, budgetId);
            budget.setItems(items);
        }
        return budget;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createBudget(Budget budget) {
        budget.setStatus(BudgetStatus.DRAFT.getCode());
        budget.setUsedAmount(BigDecimal.ZERO);
        budget.setRemainingAmount(budget.getTotalAmount());
        budget.setUsedRate(BigDecimal.ZERO);
        this.save(budget);
        if (budget.getItems() != null) {
            for (BudgetItem item : budget.getItems()) {
                item.setBudgetId(budget.getId());
                item.setBudgetCode(budget.getBudgetCode());
                item.setTenantId(budget.getTenantId());
                item.setUsedAmount(BigDecimal.ZERO);
                item.setRemainingAmount(item.getBudgetAmount());
                item.setUsedRate(BigDecimal.ZERO);
                item.setAlertFlag(0);
                budgetItemService.createItem(item);
            }
        }
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBudget(Budget budget) {
        Budget existing = this.getById(budget.getId());
        if (existing == null) {
            throw new RuntimeException("预算不存在");
        }
        if (existing.getStatus() != BudgetStatus.DRAFT.getCode() && existing.getStatus() != BudgetStatus.REJECTED.getCode()) {
            throw new RuntimeException("只有草稿或已驳回状态的预算才能修改");
        }
        return this.updateById(budget);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBudget(Long tenantId, Long budgetId) {
        Budget budget = this.getById(budgetId);
        if (budget == null) {
            throw new RuntimeException("预算不存在");
        }
        if (budget.getStatus() != BudgetStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的预算才能删除");
        }
        LambdaQueryWrapper<BudgetItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BudgetItem::getBudgetId, budgetId);
        budgetItemMapper.delete(wrapper);
        return this.removeById(budgetId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitForApproval(Long tenantId, Long budgetId) {
        Budget budget = this.getById(budgetId);
        if (budget == null) {
            throw new RuntimeException("预算不存在");
        }
        if (budget.getStatus() != BudgetStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的预算才能提交审批");
        }
        budget.setStatus(BudgetStatus.SUBMITTED.getCode());
        budget.setPreparedTime(LocalDateTime.now());
        return this.updateById(budget);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(Long tenantId, Long budgetId) {
        Budget budget = this.getById(budgetId);
        if (budget == null) {
            throw new RuntimeException("预算不存在");
        }
        if (budget.getStatus() != BudgetStatus.SUBMITTED.getCode()) {
            throw new RuntimeException("只有待审批状态的预算才能审批");
        }
        budget.setStatus(BudgetStatus.APPROVED.getCode());
        budget.setApprovedTime(LocalDateTime.now());
        return this.updateById(budget);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reject(Long tenantId, Long budgetId, String reason) {
        Budget budget = this.getById(budgetId);
        if (budget == null) {
            throw new RuntimeException("预算不存在");
        }
        if (budget.getStatus() != BudgetStatus.SUBMITTED.getCode()) {
            throw new RuntimeException("只有待审批状态的预算才能驳回");
        }
        budget.setStatus(BudgetStatus.REJECTED.getCode());
        budget.setApprovedTime(LocalDateTime.now());
        budget.setRemark(reason);
        return this.updateById(budget);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startExecution(Long tenantId, Long budgetId) {
        Budget budget = this.getById(budgetId);
        if (budget == null) {
            throw new RuntimeException("预算不存在");
        }
        if (budget.getStatus() != BudgetStatus.APPROVED.getCode()) {
            throw new RuntimeException("只有已审批状态的预算才能开始执行");
        }
        budget.setStatus(BudgetStatus.EXECUTING.getCode());
        return this.updateById(budget);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean close(Long tenantId, Long budgetId) {
        Budget budget = this.getById(budgetId);
        if (budget == null) {
            throw new RuntimeException("预算不存在");
        }
        budget.setStatus(BudgetStatus.CLOSED.getCode());
        return this.updateById(budget);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean recordExecution(Long tenantId, Long budgetItemId, BigDecimal amount, String sourceType, Long sourceId, String sourceNo) {
        BudgetItem item = budgetItemService.getById(budgetItemId);
        if (item == null) {
            throw new RuntimeException("预算明细不存在");
        }
        if (!checkBudget(tenantId, budgetItemId, amount)) {
            throw new RuntimeException("预算余额不足");
        }
        BudgetExecution execution = new BudgetExecution();
        execution.setTenantId(tenantId);
        execution.setBudgetId(item.getBudgetId());
        execution.setBudgetCode(item.getBudgetCode());
        execution.setBudgetItemId(budgetItemId);
        execution.setPeriod(item.getBudgetCode().substring(0, 6));
        execution.setExecutionDate(LocalDate.now());
        execution.setAmount(amount);
        execution.setSourceType(sourceType);
        execution.setSourceId(sourceId);
        execution.setSourceNo(sourceNo);
        budgetExecutionMapper.insert(execution);
        budgetItemService.updateUsedAmount(tenantId, budgetItemId, amount);
        Budget budget = this.getById(item.getBudgetId());
        if (budget != null) {
            BigDecimal totalUsed = budgetItemMapper.sumUsedAmountByBudgetId(tenantId, budget.getId());
            budget.setUsedAmount(totalUsed != null ? totalUsed : BigDecimal.ZERO);
            budget.setRemainingAmount(budget.getTotalAmount().subtract(budget.getUsedAmount()));
            if (budget.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                budget.setUsedRate(budget.getUsedAmount().divide(budget.getTotalAmount(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
            }
            this.updateById(budget);
        }
        return true;
    }
    
    @Override
    public boolean checkBudget(Long tenantId, Long budgetItemId, BigDecimal amount) {
        BudgetItem item = budgetItemService.getById(budgetItemId);
        if (item == null) return false;
        return item.getRemainingAmount().compareTo(amount) >= 0;
    }
    
    @Override
    public Map<String, Object> getBudgetSummary(Long tenantId, String period) {
        List<Budget> budgets = this.listByPeriod(tenantId, period);
        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalUsed = BigDecimal.ZERO;
        BigDecimal totalRemaining = BigDecimal.ZERO;
        int executingCount = 0;
        int alertCount = 0;
        for (Budget budget : budgets) {
            if (budget.getStatus() == BudgetStatus.EXECUTING.getCode()) {
                totalBudget = totalBudget.add(budget.getTotalAmount());
                totalUsed = totalUsed.add(budget.getUsedAmount());
                totalRemaining = totalRemaining.add(budget.getRemainingAmount());
                executingCount++;
            }
        }
        List<BudgetItem> alertItems = budgetItemMapper.listAlertItems(tenantId);
        alertCount = alertItems.size();
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalBudget", totalBudget);
        summary.put("totalUsed", totalUsed);
        summary.put("totalRemaining", totalRemaining);
        summary.put("usedRate", totalBudget.compareTo(BigDecimal.ZERO) > 0 ? 
            totalUsed.divide(totalBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO);
        summary.put("executingCount", executingCount);
        summary.put("alertCount", alertCount);
        return summary;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Budget> generateFromLastYear(Long tenantId, Integer year, BigDecimal adjustRate) {
        String lastYearPeriod = String.valueOf(year - 1);
        List<Budget> lastYearBudgets = this.listByPeriod(tenantId, lastYearPeriod);
        List<Budget> newBudgets = new ArrayList<>();
        for (Budget lastBudget : lastYearBudgets) {
            if (lastBudget.getStatus() != BudgetStatus.COMPLETED.getCode() && 
                lastBudget.getStatus() != BudgetStatus.CLOSED.getCode()) {
                continue;
            }
            Budget newBudget = new Budget();
            newBudget.setTenantId(tenantId);
            newBudget.setBudgetCode("BG" + year + System.currentTimeMillis());
            newBudget.setBudgetName(lastBudget.getBudgetName());
            newBudget.setBudgetType(lastBudget.getBudgetType());
            newBudget.setPeriod(String.valueOf(year));
            newBudget.setYear(year);
            newBudget.setDepartmentId(lastBudget.getDepartmentId());
            newBudget.setDepartmentName(lastBudget.getDepartmentName());
            newBudget.setProjectId(lastBudget.getProjectId());
            newBudget.setProjectName(lastBudget.getProjectName());
            BigDecimal newAmount = lastBudget.getTotalAmount().multiply(adjustRate);
            newBudget.setTotalAmount(newAmount);
            newBudget.setStatus(BudgetStatus.DRAFT.getCode());
            List<BudgetItem> lastItems = budgetItemMapper.listByBudgetId(tenantId, lastBudget.getId());
            List<BudgetItem> newItems = new ArrayList<>();
            for (BudgetItem lastItem : lastItems) {
                BudgetItem newItem = new BudgetItem();
                newItem.setTenantId(tenantId);
                newItem.setItemType(lastItem.getItemType());
                newItem.setSubjectId(lastItem.getSubjectId());
                newItem.setSubjectCode(lastItem.getSubjectCode());
                newItem.setSubjectName(lastItem.getSubjectName());
                newItem.setDepartmentId(lastItem.getDepartmentId());
                newItem.setDepartmentName(lastItem.getDepartmentName());
                newItem.setProjectId(lastItem.getProjectId());
                newItem.setProjectName(lastItem.getProjectName());
                newItem.setBudgetAmount(lastItem.getBudgetAmount().multiply(adjustRate));
                newItem.setControlRate(lastItem.getControlRate());
                newItem.setControlLevel(lastItem.getControlLevel());
                newItem.setAlertThreshold(lastItem.getAlertThreshold());
                newItems.add(newItem);
            }
            newBudget.setItems(newItems);
            this.createBudget(newBudget);
            newBudgets.add(newBudget);
        }
        return newBudgets;
    }
}