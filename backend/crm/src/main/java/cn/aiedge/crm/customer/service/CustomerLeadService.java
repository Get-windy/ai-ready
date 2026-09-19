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

    List<CustomerLead> exportList(String keyword, Integer leadStatus, Integer leadLevel,
                                   Long salesPersonId);

    Customer convertToCustomer(Long leadId);

    /**
     * 批量转化线索为客户（线索页 / 线索转化页「批量转化」）。
     * 逐条调用单条转化，失败项不中断整批，返回成功/失败明细。
     */
    java.util.Map<String, Object> batchConvertToCustomer(List<Long> leadIds);

    String generateLeadCode();
}