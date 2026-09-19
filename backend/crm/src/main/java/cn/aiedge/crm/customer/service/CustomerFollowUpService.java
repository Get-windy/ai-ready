package cn.aiedge.crm.customer.service;

import cn.aiedge.crm.customer.dto.CustomerFollowUpQuery;
import cn.aiedge.crm.customer.entity.CustomerFollowUp;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CustomerFollowUpService extends IService<CustomerFollowUp> {

    CustomerFollowUp getByFollowUpCode(String followUpCode);

    /**
     * 按查询条件分页（条件见 {@link CustomerFollowUpQuery}）。
     */
    Page<CustomerFollowUp> pageList(CustomerFollowUpQuery query);
    
    List<CustomerFollowUp> listByCustomerId(Long customerId);
    
    List<CustomerFollowUp> listByOpportunityId(Long opportunityId);
    
    List<CustomerFollowUp> listByLeadId(Long leadId);
    
    String generateFollowUpCode();
}