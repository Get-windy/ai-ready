package cn.aiedge.crm.customer.service;

import cn.aiedge.crm.customer.entity.Customer;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CustomerService extends IService<Customer> {
    
    Customer getByCustomerCode(String customerCode);
    
    Page<Customer> pageList(String keyword, Integer customerType, Integer customerLevel, 
                             Integer status, Long salesPersonId, int pageNum, int pageSize);
    
    List<Customer> listBySalesPersonId(Long salesPersonId);
    
    List<Customer> listByCustomerLevel(Integer customerLevel);
    
    String generateCustomerCode();
}