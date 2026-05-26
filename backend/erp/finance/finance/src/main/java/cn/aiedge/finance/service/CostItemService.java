package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.CostItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CostItemService extends IService<CostItem> {
    
    List<CostItem> listByPeriod(Long tenantId, String period);
    
    List<CostItem> listByCostCenterId(Long tenantId, Long costCenterId);
    
    Page<CostItem> pageList(Long tenantId, String period, Long costCenterId, Integer costType, Integer allocationStatus, Page<CostItem> page);
    
    boolean createItem(CostItem item);
    
    boolean updateItem(CostItem item);
    
    boolean deleteItem(Long tenantId, Long itemId);
    
    boolean allocate(Long tenantId, Long itemId, Long toCenterId, BigDecimal amount);
    
    boolean batchAllocate(Long tenantId, String period);
    
    BigDecimal sumAmountByPeriod(Long tenantId, String period);
    
    Map<String, BigDecimal> getCostSummary(Long tenantId, String period);
}