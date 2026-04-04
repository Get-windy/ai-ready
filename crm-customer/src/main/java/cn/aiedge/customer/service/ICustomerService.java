package cn.aiedge.customer.service;

import cn.aiedge.customer.dto.CustomerDTO;
import cn.aiedge.customer.dto.CustomerFollowDTO;
import cn.aiedge.customer.entity.Customer;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 客户服务接口
 */
public interface ICustomerService extends IService<Customer> {

    Page<CustomerDTO> pageCustomers(Page<Customer> page, Long tenantId, String customerName, 
                                     Integer stage, Integer level, Long ownerId);

    CustomerDTO getCustomerDetail(Long id);

    Long createCustomer(CustomerDTO dto);

    void updateCustomer(CustomerDTO dto);

    void deleteCustomer(Long id);

    void updateStage(Long id, Integer stage);

    List<CustomerDTO> getPendingFollowCustomers(Long ownerId);

    List<CustomerDTO> getCustomersByStage(Long tenantId, Integer stage);

    String generateCustomerCode();

    Long addFollowRecord(CustomerFollowDTO dto);

    List<CustomerFollowDTO> getFollowRecords(Long customerId);

    void transferCustomer(Long id, Long newOwnerId);
}