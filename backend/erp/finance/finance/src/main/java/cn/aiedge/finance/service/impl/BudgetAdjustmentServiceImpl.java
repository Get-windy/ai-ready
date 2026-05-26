package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.BudgetAdjustment;
import cn.aiedge.finance.entity.BudgetItem;
import cn.aiedge.finance.enums.BudgetStatus;
import cn.aiedge.finance.mapper.BudgetAdjustmentMapper;
import cn.aiedge.finance.mapper.BudgetItemMapper;
import cn.aiedge.finance.service.BudgetAdjustmentService;
import cn.aiedge.finance.service.BudgetItemService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BudgetAdjustmentServiceImpl extends ServiceImpl<BudgetAdjustmentMapper, BudgetAdjustment> implements BudgetAdjustmentService {
    
    @Autowired
    private BudgetItemMapper budgetItemMapper;
    
    @Autowired
    private BudgetItemService budgetItemService;
    
    @Override
    public List<BudgetAdjustment> listByBudgetId(Long tenantId, Long budgetId) {
        return baseMapper.listByBudgetId(tenantId, budgetId);
    }
    
    @Override
    public List<BudgetAdjustment> listByStatus(Long tenantId, Integer status) {
        return baseMapper.listByStatus(tenantId, status);
    }
    
    @Override
    public Page<BudgetAdjustment> pageList(Long tenantId, Long budgetId, Integer status, Page<BudgetAdjustment> page) {
        LambdaQueryWrapper<BudgetAdjustment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BudgetAdjustment::getTenantId, tenantId)
               .eq(BudgetAdjustment::getDeleted, 0);
        if (budgetId != null) {
            wrapper.eq(BudgetAdjustment::getBudgetId, budgetId);
        }
        if (status != null) {
            wrapper.eq(BudgetAdjustment::getStatus, status);
        }
        wrapper.orderByDesc(BudgetAdjustment::getAdjustmentDate);
        return this.page(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createAdjustment(BudgetAdjustment adjustment) {
        adjustment.setAdjustmentNo(generateAdjustmentNo(adjustment.getTenantId()));
        adjustment.setStatus(BudgetStatus.DRAFT.getCode());
        return this.save(adjustment);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitForApproval(Long tenantId, Long adjustmentId) {
        BudgetAdjustment adjustment = this.getById(adjustmentId);
        if (adjustment == null) {
            throw new RuntimeException("预算调整不存在");
        }
        if (adjustment.getStatus() != BudgetStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的调整才能提交审批");
        }
        adjustment.setStatus(BudgetStatus.SUBMITTED.getCode());
        adjustment.setPreparedTime(LocalDateTime.now());
        return this.updateById(adjustment);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(Long tenantId, Long adjustmentId) {
        BudgetAdjustment adjustment = this.getById(adjustmentId);
        if (adjustment == null) {
            throw new RuntimeException("预算调整不存在");
        }
        if (adjustment.getStatus() != BudgetStatus.SUBMITTED.getCode()) {
            throw new RuntimeException("只有待审批状态的调整才能审批");
        }
        BudgetItem item = budgetItemMapper.selectById(adjustment.getBudgetItemId());
        if (item != null) {
            item.setBudgetAmount(adjustment.getAfterAmount());
            item.setRemainingAmount(item.getBudgetAmount().subtract(item.getUsedAmount()));
            budgetItemService.updateById(item);
        }
        adjustment.setStatus(BudgetStatus.APPROVED.getCode());
        adjustment.setApprovedTime(LocalDateTime.now());
        return this.updateById(adjustment);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reject(Long tenantId, Long adjustmentId, String reason) {
        BudgetAdjustment adjustment = this.getById(adjustmentId);
        if (adjustment == null) {
            throw new RuntimeException("预算调整不存在");
        }
        if (adjustment.getStatus() != BudgetStatus.SUBMITTED.getCode()) {
            throw new RuntimeException("只有待审批状态的调整才能驳回");
        }
        adjustment.setStatus(BudgetStatus.REJECTED.getCode());
        adjustment.setApprovedTime(LocalDateTime.now());
        adjustment.setRemark(reason);
        return this.updateById(adjustment);
    }
    
    @Override
    public String generateAdjustmentNo(Long tenantId) {
        String maxNo = baseMapper.getMaxAdjustmentNo(tenantId);
        if (maxNo == null || maxNo.isEmpty()) {
            return "BA000001";
        }
        int nextNum = Integer.parseInt(maxNo.substring(2)) + 1;
        return "BA" + String.format("%06d", nextNum);
    }
}