package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.CostCenter;
import cn.aiedge.finance.mapper.CostCenterMapper;
import cn.aiedge.finance.service.CostCenterService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CostCenterServiceImpl extends ServiceImpl<CostCenterMapper, CostCenter> implements CostCenterService {
    
    @Override
    public List<CostCenter> listAllEnabled(Long tenantId) {
        return baseMapper.listAllEnabled(tenantId);
    }
    
    @Override
    public List<CostCenter> listByType(Long tenantId, Integer centerType) {
        return baseMapper.listByType(tenantId, centerType);
    }
    
    @Override
    public CostCenter getByCode(Long tenantId, String centerCode) {
        return baseMapper.getByCode(tenantId, centerCode);
    }
    
    @Override
    public Page<CostCenter> pageList(Long tenantId, String centerCode, String centerName, Integer centerType, Integer enabled, Page<CostCenter> page) {
        LambdaQueryWrapper<CostCenter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CostCenter::getTenantId, tenantId)
               .eq(CostCenter::getDeleted, 0);
        if (centerCode != null && !centerCode.isEmpty()) {
            wrapper.like(CostCenter::getCenterCode, centerCode);
        }
        if (centerName != null && !centerName.isEmpty()) {
            wrapper.like(CostCenter::getCenterName, centerName);
        }
        if (centerType != null) {
            wrapper.eq(CostCenter::getCenterType, centerType);
        }
        if (enabled != null) {
            wrapper.eq(CostCenter::getEnabled, enabled);
        }
        wrapper.orderByAsc(CostCenter::getCenterCode);
        return this.page(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createCenter(CostCenter center) {
        CostCenter existing = this.getByCode(center.getTenantId(), center.getCenterCode());
        if (existing != null) {
            throw new RuntimeException("成本中心编码已存在");
        }
        center.setEnabled(1);
        return this.save(center);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCenter(CostCenter center) {
        return this.updateById(center);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCenter(Long tenantId, Long centerId) {
        Integer childCount = baseMapper.countChildren(tenantId, centerId);
        if (childCount > 0) {
            throw new RuntimeException("存在下级成本中心，不能删除");
        }
        return this.removeById(centerId);
    }
    
    @Override
    public List<CostCenter> buildTree(Long tenantId) {
        List<CostCenter> allCenters = this.listAllEnabled(tenantId);
        Map<Long, CostCenter> centerMap = new HashMap<>();
        List<CostCenter> rootCenters = new ArrayList<>();
        for (CostCenter center : allCenters) {
            centerMap.put(center.getId(), center);
        }
        for (CostCenter center : allCenters) {
            if (center.getParentId() == null || center.getParentId() == 0) {
                rootCenters.add(center);
            } else {
                CostCenter parent = centerMap.get(center.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(center);
                }
            }
        }
        return rootCenters;
    }
}