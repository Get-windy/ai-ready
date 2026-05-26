package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.CostAllocation;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CostAllocationService extends IService<CostAllocation> {
    
    List<CostAllocation> listByPeriod(Long tenantId, String period);
    
    Page<CostAllocation> pageList(Long tenantId, String period, Long fromCenterId, Long toCenterId, Integer status, Page<CostAllocation> page);
    
    boolean createAllocation(CostAllocation allocation);
    
    boolean executeAllocation(Long tenantId, Long allocationId);
    
    boolean batchExecute(Long tenantId, String period);
    
    boolean post(Long tenantId, Long allocationId);
    
    boolean batchPost(Long tenantId, String period);
    
    BigDecimal sumAllocatedByCenter(Long tenantId, Long centerId, String period);
    
    Map<String, Object> getAllocationSummary(Long tenantId, String period);
}