package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.AuxiliaryAccounting;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AuxiliaryAccountingService extends IService<AuxiliaryAccounting> {
    
    List<AuxiliaryAccounting> listByType(Long tenantId, Integer auxiliaryType);
    
    AuxiliaryAccounting getByCode(Long tenantId, String auxiliaryCode);
    
    AuxiliaryAccounting getByTypeAndRefId(Long tenantId, Integer auxiliaryType, Long refId);
    
    Page<AuxiliaryAccounting> pageList(Long tenantId, Integer auxiliaryType, String auxiliaryCode, String auxiliaryName, Page<AuxiliaryAccounting> page);
    
    boolean createAuxiliary(AuxiliaryAccounting auxiliary);
    
    boolean updateAuxiliary(AuxiliaryAccounting auxiliary);
    
    boolean deleteAuxiliary(Long tenantId, Long auxiliaryId);
    
    boolean enableAuxiliary(Long tenantId, Long auxiliaryId);
    
    boolean disableAuxiliary(Long tenantId, Long auxiliaryId);
    
    boolean syncFromDepartment(Long tenantId);
    
    boolean syncFromCustomer(Long tenantId);
    
    boolean syncFromSupplier(Long tenantId);
}