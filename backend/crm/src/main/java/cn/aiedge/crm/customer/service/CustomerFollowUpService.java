package cn.aiedge.crm.customer.service;

import cn.aiedge.crm.customer.entity.CustomerFollowUp;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CustomerFollowUpService extends IService<CustomerFollowUp> {
    
    CustomerFollowUp getByFollowUpCode(String followUpCode);
    
    Page<CustomerFollowUp> pageList(Long customerId, Long opportunityId, Long leadId,
                                     Long salesPersonId, int pageNum, int pageSize);
    
    List<CustomerFollowUp> listByCustomerId(Long customerId);
    
    List<CustomerFollowUp> listByOpportunityId(Long opportunityId);
    
    List<CustomerFollowUp> listByLeadId(Long leadId);
    
    String generateFollowUpCode();
}