package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.BudgetItem;
import cn.aiedge.finance.mapper.BudgetItemMapper;
import cn.aiedge.finance.service.BudgetItemService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class BudgetItemServiceImpl extends ServiceImpl<BudgetItemMapper, BudgetItem> implements BudgetItemService {
    
    @Override
    public List<BudgetItem> listByBudgetId(Long tenantId, Long budgetId) {
        return baseMapper.listByBudgetId(tenantId, budgetId);
    }
    
    @Override
    public List<BudgetItem> listAlertItems(Long tenantId) {
        return baseMapper.listAlertItems(tenantId);
    }
    
    @Override
    public Page<BudgetItem> pageList(Long tenantId, Long budgetId, Long subjectId, Integer alertFlag, Page<BudgetItem> page) {
        LambdaQueryWrapper<BudgetItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BudgetItem::getTenantId, tenantId)
               .eq(BudgetItem::getDeleted, 0);
        if (budgetId != null) {
            wrapper.eq(BudgetItem::getBudgetId, budgetId);
        }
        if (subjectId != null) {
            wrapper.eq(BudgetItem::getSubjectId, subjectId);
        }
        if (alertFlag != null) {
            wrapper.eq(BudgetItem::getAlertFlag, alertFlag);
        }
        wrapper.orderByAsc(BudgetItem::getSubjectCode);
        return this.page(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createItem(BudgetItem item) {
        return this.save(item);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateItem(BudgetItem item) {
        return this.updateById(item);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteItem(Long tenantId, Long itemId) {
        return this.removeById(itemId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUsedAmount(Long tenantId, Long itemId, BigDecimal amount) {
        BudgetItem item = this.getById(itemId);
        if (item == null) {
            throw new RuntimeException("预算明细不存在");
        }
        item.setUsedAmount(item.getUsedAmount().add(amount));
        item.setRemainingAmount(item.getBudgetAmount().subtract(item.getUsedAmount()));
        if (item.getBudgetAmount().compareTo(BigDecimal.ZERO) > 0) {
            item.setUsedRate(item.getUsedAmount().divide(item.getBudgetAmount(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
        }
        checkAlert(tenantId, itemId);
        return this.updateById(item);
    }
    
    @Override
    public boolean checkAlert(Long tenantId, Long itemId) {
        BudgetItem item = this.getById(itemId);
        if (item == null) return false;
        if (item.getAlertThreshold() != null && item.getUsedRate() != null) {
            if (item.getUsedRate().compareTo(BigDecimal.valueOf(item.getAlertThreshold())) >= 0) {
                item.setAlertFlag(1);
                this.updateById(item);
                return true;
            }
        }
        return false;
    }
    
    @Override
    public BigDecimal sumBudgetAmount(Long tenantId, Long budgetId) {
        BigDecimal result = baseMapper.sumBudgetAmountByBudgetId(tenantId, budgetId);
        return result != null ? result : BigDecimal.ZERO;
    }
    
    @Override
    public BigDecimal sumUsedAmount(Long tenantId, Long budgetId) {
        BigDecimal result = baseMapper.sumUsedAmountByBudgetId(tenantId, budgetId);
        return result != null ? result : BigDecimal.ZERO;
    }
}