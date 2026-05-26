package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.AssetDepreciation;
import cn.aiedge.finance.mapper.AssetDepreciationMapper;
import cn.aiedge.finance.service.AssetDepreciationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssetDepreciationServiceImpl extends ServiceImpl<AssetDepreciationMapper, AssetDepreciation> implements AssetDepreciationService {
    
    @Override
    public List<AssetDepreciation> listByPeriod(Long tenantId, String period) {
        return baseMapper.listByPeriod(tenantId, period);
    }
    
    @Override
    public List<AssetDepreciation> listByAssetId(Long tenantId, Long assetId) {
        return baseMapper.listByAssetId(tenantId, assetId);
    }
    
    @Override
    public AssetDepreciation getByAssetIdAndPeriod(Long tenantId, Long assetId, String period) {
        return baseMapper.getByAssetIdAndPeriod(tenantId, assetId, period);
    }
    
    @Override
    public Page<AssetDepreciation> pageList(Long tenantId, String period, String assetCode, Integer status, Page<AssetDepreciation> page) {
        LambdaQueryWrapper<AssetDepreciation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetDepreciation::getTenantId, tenantId)
               .eq(AssetDepreciation::getDeleted, 0);
        if (period != null && !period.isEmpty()) {
            wrapper.eq(AssetDepreciation::getPeriod, period);
        }
        if (assetCode != null && !assetCode.isEmpty()) {
            wrapper.like(AssetDepreciation::getAssetCode, assetCode);
        }
        if (status != null) {
            wrapper.eq(AssetDepreciation::getStatus, status);
        }
        wrapper.orderByDesc(AssetDepreciation::getPeriod).orderByAsc(AssetDepreciation::getAssetCode);
        return this.page(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createDepreciation(AssetDepreciation depreciation) {
        return this.save(depreciation);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean postDepreciation(Long tenantId, Long depreciationId) {
        AssetDepreciation depreciation = this.getById(depreciationId);
        if (depreciation == null) {
            throw new RuntimeException("折旧记录不存在");
        }
        if (depreciation.getStatus() != 0) {
            throw new RuntimeException("折旧记录已记账");
        }
        depreciation.setStatus(1);
        return this.updateById(depreciation);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchPostDepreciation(Long tenantId, String period) {
        List<AssetDepreciation> depreciations = this.listByPeriod(tenantId, period);
        for (AssetDepreciation dep : depreciations) {
            if (dep.getStatus() == 0) {
                dep.setStatus(1);
                this.updateById(dep);
            }
        }
        return true;
    }
    
    @Override
    public BigDecimal sumPeriodDepreciation(Long tenantId, String period) {
        BigDecimal result = baseMapper.sumPeriodDepreciation(tenantId, period);
        return result != null ? result : BigDecimal.ZERO;
    }
    
    @Override
    public Map<String, Object> getDepreciationSummary(Long tenantId, String period) {
        List<AssetDepreciation> depreciations = this.listByPeriod(tenantId, period);
        BigDecimal totalDepreciation = BigDecimal.ZERO;
        int postedCount = 0;
        int unpostedCount = 0;
        for (AssetDepreciation dep : depreciations) {
            totalDepreciation = totalDepreciation.add(dep.getPeriodDepreciation());
            if (dep.getStatus() == 1) {
                postedCount++;
            } else {
                unpostedCount++;
            }
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalDepreciation", totalDepreciation);
        summary.put("assetCount", depreciations.size());
        summary.put("postedCount", postedCount);
        summary.put("unpostedCount", unpostedCount);
        return summary;
    }
}