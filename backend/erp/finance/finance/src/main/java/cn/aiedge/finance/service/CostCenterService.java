package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.CostCenter;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CostCenterService extends IService<CostCenter> {
    
    List<CostCenter> listAllEnabled(Long tenantId);
    
    List<CostCenter> listByType(Long tenantId, Integer centerType);
    
    CostCenter getByCode(Long tenantId, String centerCode);
    
    Page<CostCenter> pageList(Long tenantId, String centerCode, String centerName, Integer centerType, Integer enabled, Page<CostCenter> page);
    
    boolean createCenter(CostCenter center);
    
    boolean updateCenter(CostCenter center);
    
    boolean deleteCenter(Long tenantId, Long centerId);
    
    List<CostCenter> buildTree(Long tenantId);
}