package cn.aiedge.crm.customer.service;

import cn.aiedge.crm.customer.entity.CustomerPool;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CustomerPoolService extends IService<CustomerPool> {

    CustomerPool getByCustomerId(Long customerId);

    Page<CustomerPool> pageList(String keyword, Integer poolType, Integer status, int pageNum, int pageSize);

    List<CustomerPool> listAvailable();

    List<CustomerPool> listByClaimSalesPerson(Long salesPersonId);

    List<CustomerPool> listByOriginalSalesPerson(Long salesPersonId);

    CustomerPool putToPool(Long customerId, Integer poolReason, String remark);

    CustomerPool claimFromPool(Long poolId, Long salesPersonId);

    CustomerPool returnToPool(Long poolId, String remark);

    void autoRecovery(Integer noFollowUpDays);

    void checkExpired();

    Integer getAvailableCount();

    Integer getClaimCountBySalesPerson(Long salesPersonId);
}