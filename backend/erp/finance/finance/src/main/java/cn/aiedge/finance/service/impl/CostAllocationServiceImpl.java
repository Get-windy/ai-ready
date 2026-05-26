package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.CostAllocation;
import cn.aiedge.finance.mapper.CostAllocationMapper;
import cn.aiedge.finance.service.CostAllocationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CostAllocationServiceImpl extends ServiceImpl<CostAllocationMapper, CostAllocation> implements CostAllocationService {
    
    @Override
    public List<CostAllocation> listByPeriod(Long tenantId, String period) {
        return baseMapper.listByPeriod(tenantId, period);
    }
    
    @Override
    public Page<CostAllocation> pageList(Long tenantId, String period, Long fromCenterId, Long toCenterId, Integer status, Page<CostAllocation> page) {
        LambdaQueryWrapper<CostAllocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CostAllocation::getTenantId, tenantId)
               .eq(CostAllocation::getDeleted, 0);
        if (period != null && !period.isEmpty()) {
            wrapper.eq(CostAllocation::getPeriod, period);
        }
        if (fromCenterId != null) {
            wrapper.eq(CostAllocation::getFromCenterId, fromCenterId);
        }
        if (toCenterId != null) {
            wrapper.eq(CostAllocation::getToCenterId, toCenterId);
        }
        if (status != null) {
            wrapper.eq(CostAllocation::getStatus, status);
        }
        wrapper.orderByDesc(CostAllocation::getAllocationDate);
        return this.page(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createAllocation(CostAllocation allocation) {
        String allocationNo = "CA" + System.currentTimeMillis();
        allocation.setAllocationNo(allocationNo);
        allocation.setStatus(0);
        return this.save(allocation);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean executeAllocation(Long tenantId, Long allocationId) {
        CostAllocation allocation = this.getById(allocationId);
        if (allocation == null) {
            throw new RuntimeException("成本分配记录不存在");
        }
        allocation.setStatus(1);
        return this.updateById(allocation);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchExecute(Long tenantId, String period) {
        List<CostAllocation> allocations = this.listByPeriod(tenantId, period);
        for (CostAllocation allocation : allocations) {
            if (allocation.getStatus() == 0) {
                allocation.setStatus(1);
                this.updateById(allocation);
            }
        }
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean post(Long tenantId, Long allocationId) {
        CostAllocation allocation = this.getById(allocationId);
        if (allocation == null) {
            throw new RuntimeException("成本分配记录不存在");
        }
        if (allocation.getStatus() != 1) {
            throw new RuntimeException("只有已执行的分配才能记账");
        }
        allocation.setStatus(2);
        return this.updateById(allocation);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchPost(Long tenantId, String period) {
        List<CostAllocation> allocations = this.listByPeriod(tenantId, period);
        for (CostAllocation allocation : allocations) {
            if (allocation.getStatus() == 1) {
                allocation.setStatus(2);
                this.updateById(allocation);
            }
        }
        return true;
    }
    
    @Override
    public BigDecimal sumAllocatedByCenter(Long tenantId, Long centerId, String period) {
        BigDecimal result = baseMapper.sumAllocatedByToCenterAndPeriod(tenantId, centerId, period);
        return result != null ? result : BigDecimal.ZERO;
    }
    
    @Override
    public Map<String, Object> getAllocationSummary(Long tenantId, String period) {
        List<CostAllocation> allocations = this.listByPeriod(tenantId, period);
        BigDecimal totalAllocated = BigDecimal.ZERO;
        int executedCount = 0;
        int postedCount = 0;
        int pendingCount = 0;
        for (CostAllocation allocation : allocations) {
            totalAllocated = totalAllocated.add(allocation.getAllocatedAmount());
            switch (allocation.getStatus()) {
                case 0: pendingCount++; break;
                case 1: executedCount++; break;
                case 2: postedCount++; break;
            }
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAllocated", totalAllocated);
        summary.put("allocationCount", allocations.size());
        summary.put("pendingCount", pendingCount);
        summary.put("executedCount", executedCount);
        summary.put("postedCount", postedCount);
        return summary;
    }
}