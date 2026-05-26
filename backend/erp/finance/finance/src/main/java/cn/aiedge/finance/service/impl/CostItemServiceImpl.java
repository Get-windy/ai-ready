package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.CostAllocation;
import cn.aiedge.finance.entity.CostCenter;
import cn.aiedge.finance.entity.CostItem;
import cn.aiedge.finance.mapper.CostAllocationMapper;
import cn.aiedge.finance.mapper.CostCenterMapper;
import cn.aiedge.finance.mapper.CostItemMapper;
import cn.aiedge.finance.service.CostAllocationService;
import cn.aiedge.finance.service.CostItemService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CostItemServiceImpl extends ServiceImpl<CostItemMapper, CostItem> implements CostItemService {
    
    @Autowired
    private CostCenterMapper costCenterMapper;
    
    @Autowired
    private CostAllocationMapper costAllocationMapper;
    
    @Autowired
    private CostAllocationService costAllocationService;
    
    @Override
    public List<CostItem> listByPeriod(Long tenantId, String period) {
        return baseMapper.listByPeriod(tenantId, period);
    }
    
    @Override
    public List<CostItem> listByCostCenterId(Long tenantId, Long costCenterId) {
        return baseMapper.listByCostCenterId(tenantId, costCenterId);
    }
    
    @Override
    public Page<CostItem> pageList(Long tenantId, String period, Long costCenterId, Integer costType, Integer allocationStatus, Page<CostItem> page) {
        LambdaQueryWrapper<CostItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CostItem::getTenantId, tenantId)
               .eq(CostItem::getDeleted, 0);
        if (period != null && !period.isEmpty()) {
            wrapper.eq(CostItem::getPeriod, period);
        }
        if (costCenterId != null) {
            wrapper.eq(CostItem::getCostCenterId, costCenterId);
        }
        if (costType != null) {
            wrapper.eq(CostItem::getCostType, costType);
        }
        if (allocationStatus != null) {
            wrapper.eq(CostItem::getAllocationStatus, allocationStatus);
        }
        wrapper.orderByAsc(CostItem::getItemCode);
        return this.page(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createItem(CostItem item) {
        item.setAllocatedAmount(BigDecimal.ZERO);
        item.setUnallocatedAmount(item.getAmount());
        item.setAllocationStatus(0);
        return this.save(item);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateItem(CostItem item) {
        CostItem existing = this.getById(item.getId());
        if (existing == null) {
            throw new RuntimeException("成本项目不存在");
        }
        item.setUnallocatedAmount(item.getAmount().subtract(existing.getAllocatedAmount()));
        return this.updateById(item);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteItem(Long tenantId, Long itemId) {
        CostItem item = this.getById(itemId);
        if (item == null) {
            throw new RuntimeException("成本项目不存在");
        }
        if (item.getAllocationStatus() == 1) {
            throw new RuntimeException("已分配的成本项目不能删除");
        }
        return this.removeById(itemId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean allocate(Long tenantId, Long itemId, Long toCenterId, BigDecimal amount) {
        CostItem item = this.getById(itemId);
        if (item == null) {
            throw new RuntimeException("成本项目不存在");
        }
        if (amount.compareTo(item.getUnallocatedAmount()) > 0) {
            throw new RuntimeException("分配金额超过未分配金额");
        }
        CostCenter toCenter = costCenterMapper.selectById(toCenterId);
        if (toCenter == null) {
            throw new RuntimeException("目标成本中心不存在");
        }
        CostAllocation allocation = new CostAllocation();
        allocation.setTenantId(tenantId);
        allocation.setPeriod(item.getPeriod());
        allocation.setAllocationDate(LocalDate.now());
        allocation.setFromCenterId(item.getCostCenterId());
        allocation.setFromCenterCode(item.getCostCenterCode());
        allocation.setFromCenterName(item.getCostCenterName());
        allocation.setToCenterId(toCenterId);
        allocation.setToCenterCode(toCenter.getCenterCode());
        allocation.setToCenterName(toCenter.getCenterName());
        allocation.setCostItemId(itemId);
        allocation.setCostItemCode(item.getItemCode());
        allocation.setCostItemName(item.getItemName());
        allocation.setCostType(item.getCostType());
        allocation.setOriginalAmount(item.getAmount());
        allocation.setAllocatedAmount(amount);
        allocation.setStatus(0);
        costAllocationService.createAllocation(allocation);
        item.setAllocatedAmount(item.getAllocatedAmount().add(amount));
        item.setUnallocatedAmount(item.getUnallocatedAmount().subtract(amount));
        if (item.getUnallocatedAmount().compareTo(BigDecimal.ZERO) == 0) {
            item.setAllocationStatus(1);
        }
        return this.updateById(item);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchAllocate(Long tenantId, String period) {
        List<CostItem> items = baseMapper.listByPeriod(tenantId, period);
        for (CostItem item : items) {
            if (item.getAllocationStatus() == 0 && item.getUnallocatedAmount().compareTo(BigDecimal.ZERO) > 0) {
                List<CostCenter> toCenters = costCenterMapper.listByType(tenantId, 3);
                if (!toCenters.isEmpty()) {
                    BigDecimal amountPerCenter = item.getUnallocatedAmount().divide(BigDecimal.valueOf(toCenters.size()), 2, RoundingMode.HALF_UP);
                    for (CostCenter toCenter : toCenters) {
                        this.allocate(tenantId, item.getId(), toCenter.getId(), amountPerCenter);
                    }
                }
            }
        }
        return true;
    }
    
    @Override
    public BigDecimal sumAmountByPeriod(Long tenantId, String period) {
        BigDecimal result = baseMapper.sumAmountByPeriod(tenantId, period);
        return result != null ? result : BigDecimal.ZERO;
    }
    
    @Override
    public Map<String, BigDecimal> getCostSummary(Long tenantId, String period) {
        List<CostItem> items = this.listByPeriod(tenantId, period);
        Map<String, BigDecimal> summary = new HashMap<>();
        BigDecimal totalDirectMaterial = BigDecimal.ZERO;
        BigDecimal totalDirectLabor = BigDecimal.ZERO;
        BigDecimal totalManufacturing = BigDecimal.ZERO;
        BigDecimal totalOther = BigDecimal.ZERO;
        for (CostItem item : items) {
            switch (item.getCostType()) {
                case 1: totalDirectMaterial = totalDirectMaterial.add(item.getAmount()); break;
                case 2: totalDirectLabor = totalDirectLabor.add(item.getAmount()); break;
                case 3: totalManufacturing = totalManufacturing.add(item.getAmount()); break;
                default: totalOther = totalOther.add(item.getAmount()); break;
            }
        }
        summary.put("totalDirectMaterial", totalDirectMaterial);
        summary.put("totalDirectLabor", totalDirectLabor);
        summary.put("totalManufacturing", totalManufacturing);
        summary.put("totalOther", totalOther);
        summary.put("totalCost", totalDirectMaterial.add(totalDirectLabor).add(totalManufacturing).add(totalOther));
        return summary;
    }
}