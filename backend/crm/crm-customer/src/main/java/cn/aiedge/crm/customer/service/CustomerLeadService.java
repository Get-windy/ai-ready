package cn.aiedge.crm.customer.service;

import cn.aiedge.crm.customer.entity.Customer;
import cn.aiedge.crm.customer.entity.CustomerLead;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CustomerLeadService extends IService<CustomerLead> {
    
    CustomerLead getByLeadCode(String leadCode);
    
    Page<CustomerLead> pageList(String keyword, Integer leadStatus, Integer leadLevel,
                                 Long salesPersonId, int pageNum, int pageSize);
    
    List<CustomerLead> listBySalesPersonId(Long salesPersonId);
    
    Customer convertToCustomer(Long leadId);
    
    String generateLeadCode();
}