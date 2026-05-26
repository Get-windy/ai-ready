package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.AssetChange;
import cn.aiedge.finance.entity.FixedAsset;
import cn.aiedge.finance.enums.AssetChangeType;
import cn.aiedge.finance.mapper.AssetChangeMapper;
import cn.aiedge.finance.mapper.FixedAssetMapper;
import cn.aiedge.finance.service.AssetChangeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class AssetChangeServiceImpl extends ServiceImpl<AssetChangeMapper, AssetChange> implements AssetChangeService {
    
    @Autowired
    private FixedAssetMapper fixedAssetMapper;
    
    @Override
    public List<AssetChange> listByAssetId(Long tenantId, Long assetId) {
        return baseMapper.listByAssetId(tenantId, assetId);
    }
    
    @Override
    public List<AssetChange> listByChangeType(Long tenantId, Integer changeType) {
        return baseMapper.listByChangeType(tenantId, changeType);
    }
    
    @Override
    public Page<AssetChange> pageList(Long tenantId, Long assetId, Integer changeType, String startDate, String endDate, Page<AssetChange> page) {
        LambdaQueryWrapper<AssetChange> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetChange::getTenantId, tenantId)
               .eq(AssetChange::getDeleted, 0);
        if (assetId != null) {
            wrapper.eq(AssetChange::getAssetId, assetId);
        }
        if (changeType != null) {
            wrapper.eq(AssetChange::getChangeType, changeType);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(AssetChange::getChangeDate, LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(AssetChange::getChangeDate, LocalDate.parse(endDate));
        }
        wrapper.orderByDesc(AssetChange::getChangeDate);
        return this.page(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createChange(AssetChange change) {
        return this.save(change);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean recordTransfer(Long tenantId, Long assetId, Long toDepartmentId, Long toLocationId, Long toCustodianId, String reason) {
        FixedAsset asset = fixedAssetMapper.selectById(assetId);
        if (asset == null) {
            throw new RuntimeException("资产不存在");
        }
        AssetChange change = new AssetChange();
        change.setAssetId(assetId);
        change.setAssetCode(asset.getAssetCode());
        change.setAssetName(asset.getAssetName());
        change.setChangeType(AssetChangeType.TRANSFER.getCode());
        change.setChangeDate(LocalDate.now());
        change.setBeforeDepartmentId(asset.getDepartmentId());
        change.setBeforeDepartmentName(asset.getDepartmentName());
        change.setAfterDepartmentId(toDepartmentId);
        change.setBeforeLocationId(asset.getLocationId());
        change.setBeforeLocationName(asset.getLocationName());
        change.setAfterLocationId(toLocationId);
        change.setBeforeCustodianId(asset.getCustodianId());
        change.setBeforeCustodianName(asset.getCustodianName());
        change.setAfterCustodianId(toCustodianId);
        change.setReason(reason);
        change.setTenantId(tenantId);
        return this.save(change);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean recordDispose(Long tenantId, Long assetId, String reason) {
        FixedAsset asset = fixedAssetMapper.selectById(assetId);
        if (asset == null) {
            throw new RuntimeException("资产不存在");
        }
        AssetChange change = new AssetChange();
        change.setAssetId(assetId);
        change.setAssetCode(asset.getAssetCode());
        change.setAssetName(asset.getAssetName());
        change.setChangeType(AssetChangeType.DISPOSE.getCode());
        change.setChangeDate(LocalDate.now());
        change.setBeforeStatus(asset.getStatus());
        change.setAfterStatus(4);
        change.setBeforeValue(asset.getNetValue());
        change.setAfterValue(BigDecimal.ZERO);
        change.setChangeAmount(asset.getNetValue());
        change.setReason(reason);
        change.setTenantId(tenantId);
        return this.save(change);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean recordScrap(Long tenantId, Long assetId, String reason) {
        FixedAsset asset = fixedAssetMapper.selectById(assetId);
        if (asset == null) {
            throw new RuntimeException("资产不存在");
        }
        AssetChange change = new AssetChange();
        change.setAssetId(assetId);
        change.setAssetCode(asset.getAssetCode());
        change.setAssetName(asset.getAssetName());
        change.setChangeType(AssetChangeType.SCRAP.getCode());
        change.setChangeDate(LocalDate.now());
        change.setBeforeStatus(asset.getStatus());
        change.setAfterStatus(5);
        change.setBeforeValue(asset.getNetValue());
        change.setAfterValue(BigDecimal.ZERO);
        change.setChangeAmount(asset.getNetValue());
        change.setReason(reason);
        change.setTenantId(tenantId);
        return this.save(change);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean recordDepreciation(Long tenantId, Long assetId, BigDecimal depreciationAmount, String period) {
        FixedAsset asset = fixedAssetMapper.selectById(assetId);
        if (asset == null) {
            throw new RuntimeException("资产不存在");
        }
        AssetChange change = new AssetChange();
        change.setAssetId(assetId);
        change.setAssetCode(asset.getAssetCode());
        change.setAssetName(asset.getAssetName());
        change.setChangeType(AssetChangeType.DEPRECIATION.getCode());
        change.setChangeDate(LocalDate.now());
        change.setBeforeValue(asset.getAccumulatedDepreciation());
        change.setAfterValue(asset.getAccumulatedDepreciation().add(depreciationAmount));
        change.setChangeAmount(depreciationAmount);
        change.setReason("期间折旧：" + period);
        change.setTenantId(tenantId);
        return this.save(change);
    }
}