package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.AuxiliaryAccounting;
import cn.aiedge.finance.mapper.AuxiliaryAccountingMapper;
import cn.aiedge.finance.service.AuxiliaryAccountingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuxiliaryAccountingServiceImpl extends ServiceImpl<AuxiliaryAccountingMapper, AuxiliaryAccounting> implements AuxiliaryAccountingService {
    
    @Override
    public List<AuxiliaryAccounting> listByType(Long tenantId, Integer auxiliaryType) {
        return baseMapper.listByType(tenantId, auxiliaryType);
    }
    
    @Override
    public AuxiliaryAccounting getByCode(Long tenantId, String auxiliaryCode) {
        return baseMapper.getByCode(tenantId, auxiliaryCode);
    }
    
    @Override
    public AuxiliaryAccounting getByTypeAndRefId(Long tenantId, Integer auxiliaryType, Long refId) {
        return baseMapper.getByTypeAndRefId(tenantId, auxiliaryType, refId);
    }
    
    @Override
    public Page<AuxiliaryAccounting> pageList(Long tenantId, Integer auxiliaryType, String auxiliaryCode, String auxiliaryName, Page<AuxiliaryAccounting> page) {
        LambdaQueryWrapper<AuxiliaryAccounting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuxiliaryAccounting::getTenantId, tenantId)
               .eq(AuxiliaryAccounting::getDeleted, 0);
        if (auxiliaryType != null) {
            wrapper.eq(AuxiliaryAccounting::getAuxiliaryType, auxiliaryType);
        }
        if (auxiliaryCode != null && !auxiliaryCode.isEmpty()) {
            wrapper.like(AuxiliaryAccounting::getAuxiliaryCode, auxiliaryCode);
        }
        if (auxiliaryName != null && !auxiliaryName.isEmpty()) {
            wrapper.like(AuxiliaryAccounting::getAuxiliaryName, auxiliaryName);
        }
        wrapper.orderByAsc(AuxiliaryAccounting::getAuxiliaryCode);
        return this.page(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createAuxiliary(AuxiliaryAccounting auxiliary) {
        AuxiliaryAccounting existing = this.getByCode(auxiliary.getTenantId(), auxiliary.getAuxiliaryCode());
        if (existing != null) {
            throw new RuntimeException("辅助核算编码已存在");
        }
        auxiliary.setEnabled(1);
        return this.save(auxiliary);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAuxiliary(AuxiliaryAccounting auxiliary) {
        AuxiliaryAccounting existing = this.getById(auxiliary.getId());
        if (existing == null) {
            throw new RuntimeException("辅助核算不存在");
        }
        return this.updateById(auxiliary);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAuxiliary(Long tenantId, Long auxiliaryId) {
        return this.removeById(auxiliaryId);
    }
    
    @Override
    public boolean enableAuxiliary(Long tenantId, Long auxiliaryId) {
        AuxiliaryAccounting auxiliary = this.getById(auxiliaryId);
        if (auxiliary == null) {
            throw new RuntimeException("辅助核算不存在");
        }
        auxiliary.setEnabled(1);
        return this.updateById(auxiliary);
    }
    
    @Override
    public boolean disableAuxiliary(Long tenantId, Long auxiliaryId) {
        AuxiliaryAccounting auxiliary = this.getById(auxiliaryId);
        if (auxiliary == null) {
            throw new RuntimeException("辅助核算不存在");
        }
        auxiliary.setEnabled(0);
        return this.updateById(auxiliary);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncFromDepartment(Long tenantId) {
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncFromCustomer(Long tenantId) {
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncFromSupplier(Long tenantId) {
        return true;
    }
}