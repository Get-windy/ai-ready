package cn.aiedge.crm.customer.service;

import cn.aiedge.crm.customer.entity.CustomerOpportunity;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CustomerOpportunityService extends IService<CustomerOpportunity> {
    
    CustomerOpportunity getByOpportunityCode(String opportunityCode);
    
    Page<CustomerOpportunity> pageList(String keyword, Long customerId, Integer opportunityStage,
                                        Integer status, Long salesPersonId, int pageNum, int pageSize);
    
    List<CustomerOpportunity> listByCustomerId(Long customerId);
    
    List<CustomerOpportunity> listBySalesPersonId(Long salesPersonId);

    List<CustomerOpportunity> exportList(String keyword, Long customerId, Integer opportunityStage,
                                          Integer status, Long salesPersonId);

    CustomerOpportunity advanceStage(Long opportunityId);

    /**
     * 跳转到指定阶段（商机看板拖拽改阶段）。
     * 与 advanceStage 的区别：不做「只能 +1」限制，允许前后移动。
     */
    CustomerOpportunity updateStage(Long opportunityId, Integer stage);

    CustomerOpportunity winOpportunity(Long opportunityId, BigDecimal actualAmount);
    
    CustomerOpportunity loseOpportunity(Long opportunityId, String loseReason);
    
    Map<String, Object> getOpportunityStatistics(Long salesPersonId);
    
    String generateOpportunityCode();
}