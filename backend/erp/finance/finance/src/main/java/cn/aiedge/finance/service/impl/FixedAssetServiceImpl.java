package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.AssetChange;
import cn.aiedge.finance.entity.AssetDepreciation;
import cn.aiedge.finance.entity.FixedAsset;
import cn.aiedge.finance.enums.AssetStatus;
import cn.aiedge.finance.enums.DepreciationMethod;
import cn.aiedge.finance.mapper.AssetChangeMapper;
import cn.aiedge.finance.mapper.AssetDepreciationMapper;
import cn.aiedge.finance.mapper.FixedAssetMapper;
import cn.aiedge.finance.service.AssetChangeService;
import cn.aiedge.finance.service.AssetDepreciationService;
import cn.aiedge.finance.service.FixedAssetService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FixedAssetServiceImpl extends ServiceImpl<FixedAssetMapper, FixedAsset> implements FixedAssetService {
    
    @Autowired
    private AssetDepreciationMapper assetDepreciationMapper;
    
    @Autowired
    private AssetDepreciationService assetDepreciationService;
    
    @Autowired
    private AssetChangeMapper assetChangeMapper;
    
    @Autowired
    private AssetChangeService assetChangeService;
    
    @Override
    public List<FixedAsset> listAll(Long tenantId) {
        return baseMapper.listAll(tenantId);
    }
    
    @Override
    public List<FixedAsset> listByCategoryId(Long tenantId, Long categoryId) {
        return baseMapper.listByCategoryId(tenantId, categoryId);
    }
    
    @Override
    public List<FixedAsset> listByStatus(Long tenantId, Integer status) {
        return baseMapper.listByStatus(tenantId, status);
    }
    
    @Override
    public List<FixedAsset> listByDepartmentId(Long tenantId, Long departmentId) {
        return baseMapper.listByDepartmentId(tenantId, departmentId);
    }
    
    @Override
    public FixedAsset getByCode(Long tenantId, String assetCode) {
        return baseMapper.getByCode(tenantId, assetCode);
    }
    
    @Override
    public Page<FixedAsset> pageList(Long tenantId, String assetCode, String assetName, Long categoryId, Integer status, Long departmentId, Page<FixedAsset> page) {
        LambdaQueryWrapper<FixedAsset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FixedAsset::getTenantId, tenantId)
               .eq(FixedAsset::getDeleted, 0);
        if (assetCode != null && !assetCode.isEmpty()) {
            wrapper.like(FixedAsset::getAssetCode, assetCode);
        }
        if (assetName != null && !assetName.isEmpty()) {
            wrapper.like(FixedAsset::getAssetName, assetName);
        }
        if (categoryId != null) {
            wrapper.eq(FixedAsset::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(FixedAsset::getStatus, status);
        }
        if (departmentId != null) {
            wrapper.eq(FixedAsset::getDepartmentId, departmentId);
        }
        wrapper.orderByAsc(FixedAsset::getAssetCode);
        return this.page(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createAsset(FixedAsset asset) {
        if (asset.getAssetCode() == null || asset.getAssetCode().isEmpty()) {
            asset.setAssetCode(generateAssetCode(asset.getTenantId()));
        }
        asset.setStatus(AssetStatus.NORMAL.getCode());
        asset.setAccumulatedDepreciation(BigDecimal.ZERO);
        asset.setUsedMonths(0);
        BigDecimal netValue = asset.getOriginalValue().subtract(asset.getResidualValue());
        asset.setNetValue(netValue);
        if (asset.getDepreciationMethod() == DepreciationMethod.STRAIGHT_LINE.getCode()) {
            BigDecimal monthlyDep = netValue.divide(BigDecimal.valueOf(asset.getUsefulLife() * 12), 2, RoundingMode.HALF_UP);
            BigDecimal rate = monthlyDep.divide(asset.getOriginalValue(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            asset.setDepreciationRate(rate);
        }
        this.save(asset);
        AssetChange change = new AssetChange();
        change.setAssetId(asset.getId());
        change.setAssetCode(asset.getAssetCode());
        change.setAssetName(asset.getAssetName());
        change.setChangeType(1);
        change.setChangeDate(LocalDate.now());
        change.setBeforeValue(BigDecimal.ZERO);
        change.setAfterValue(asset.getOriginalValue());
        change.setChangeAmount(asset.getOriginalValue());
        change.setAfterStatus(AssetStatus.NORMAL.getCode());
        change.setAfterDepartmentId(asset.getDepartmentId());
        change.setAfterDepartmentName(asset.getDepartmentName());
        change.setTenantId(asset.getTenantId());
        assetChangeService.createChange(change);
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAsset(FixedAsset asset) {
        FixedAsset existing = this.getById(asset.getId());
        if (existing == null) {
            throw new RuntimeException("资产不存在");
        }
        return this.updateById(asset);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAsset(Long tenantId, Long assetId) {
        FixedAsset asset = this.getById(assetId);
        if (asset == null) {
            throw new RuntimeException("资产不存在");
        }
        if (asset.getStatus() == AssetStatus.NORMAL.getCode()) {
            throw new RuntimeException("正常使用的资产不能删除");
        }
        return this.removeById(assetId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean transfer(Long tenantId, Long assetId, Long toDepartmentId, Long toLocationId, Long toCustodianId, String reason) {
        FixedAsset asset = this.getById(assetId);
        if (asset == null) {
            throw new RuntimeException("资产不存在");
        }
        if (asset.getStatus() != AssetStatus.NORMAL.getCode()) {
            throw new RuntimeException("只有正常使用的资产才能转移");
        }
        asset.setDepartmentId(toDepartmentId);
        asset.setLocationId(toLocationId);
        asset.setCustodianId(toCustodianId);
        this.updateById(asset);
        assetChangeService.recordTransfer(tenantId, assetId, toDepartmentId, toLocationId, toCustodianId, reason);
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean dispose(Long tenantId, Long assetId, BigDecimal disposeValue, String reason) {
        FixedAsset asset = this.getById(assetId);
        if (asset == null) {
            throw new RuntimeException("资产不存在");
        }
        asset.setStatus(AssetStatus.DISPOSED.getCode());
        this.updateById(asset);
        assetChangeService.recordDispose(tenantId, assetId, reason);
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean scrap(Long tenantId, Long assetId, String reason) {
        FixedAsset asset = this.getById(assetId);
        if (asset == null) {
            throw new RuntimeException("资产不存在");
        }
        asset.setStatus(AssetStatus.SCRAPPED.getCode());
        this.updateById(asset);
        assetChangeService.recordScrap(tenantId, assetId, reason);
        return true;
    }
    
    @Override
    public BigDecimal calculateDepreciation(FixedAsset asset) {
        BigDecimal depreciation = BigDecimal.ZERO;
        BigDecimal depreciableValue = asset.getOriginalValue().subtract(asset.getResidualValue());
        int remainingMonths = asset.getUsefulLife() * 12 - asset.getUsedMonths();
        if (remainingMonths <= 0) {
            return BigDecimal.ZERO;
        }
        switch (DepreciationMethod.fromCode(asset.getDepreciationMethod())) {
            case STRAIGHT_LINE:
                depreciation = depreciableValue.divide(BigDecimal.valueOf(asset.getUsefulLife() * 12), 2, RoundingMode.HALF_UP);
                break;
            case DOUBLE_DECLINING:
                BigDecimal bookValue = asset.getOriginalValue().subtract(asset.getAccumulatedDepreciation());
                BigDecimal rate = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(asset.getUsefulLife()), 4, RoundingMode.HALF_UP);
                depreciation = bookValue.multiply(rate).divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
                if (depreciation.compareTo(bookValue.subtract(asset.getResidualValue()).divide(BigDecimal.valueOf(remainingMonths), 2, RoundingMode.HALF_UP)) > 0) {
                    depreciation = bookValue.subtract(asset.getResidualValue()).divide(BigDecimal.valueOf(remainingMonths), 2, RoundingMode.HALF_UP);
                }
                break;
            case SUM_OF_YEARS:
                int totalYears = asset.getUsefulLife();
                int currentYear = asset.getUsedMonths() / 12 + 1;
                int sumOfYears = totalYears * (totalYears + 1) / 2;
                BigDecimal yearFactor = BigDecimal.valueOf(totalYears - currentYear + 1).divide(BigDecimal.valueOf(sumOfYears), 4, RoundingMode.HALF_UP);
                depreciation = depreciableValue.multiply(yearFactor).divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
                break;
            default:
                depreciation = BigDecimal.ZERO;
        }
        return depreciation;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean runDepreciation(Long tenantId, String period) {
        List<FixedAsset> assets = baseMapper.listByStatus(tenantId, AssetStatus.NORMAL.getCode());
        for (FixedAsset asset : assets) {
            AssetDepreciation existing = assetDepreciationMapper.getByAssetIdAndPeriod(tenantId, asset.getId(), period);
            if (existing != null) {
                continue;
            }
            BigDecimal depreciation = calculateDepreciation(asset);
            if (depreciation.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            AssetDepreciation depRecord = new AssetDepreciation();
            depRecord.setAssetId(asset.getId());
            depRecord.setAssetCode(asset.getAssetCode());
            depRecord.setAssetName(asset.getAssetName());
            depRecord.setPeriod(period);
            depRecord.setDepreciationDate(LocalDate.now());
            depRecord.setOriginalValue(asset.getOriginalValue());
            depRecord.setAccumulatedDepreciation(asset.getAccumulatedDepreciation());
            depRecord.setPeriodDepreciation(depreciation);
            BigDecimal newAccumulated = asset.getAccumulatedDepreciation().add(depreciation);
            depRecord.setNetValue(asset.getOriginalValue().subtract(newAccumulated));
            depRecord.setDepreciationRate(asset.getDepreciationRate());
            depRecord.setUsedMonths(asset.getUsedMonths() + 1);
            depRecord.setRemainingMonths(asset.getUsefulLife() * 12 - asset.getUsedMonths() - 1);
            depRecord.setStatus(0);
            depRecord.setTenantId(tenantId);
            assetDepreciationService.createDepreciation(depRecord);
        }
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean postDepreciation(Long tenantId, String period) {
        List<AssetDepreciation> depreciations = assetDepreciationMapper.listByPeriod(tenantId, period);
        for (AssetDepreciation dep : depreciations) {
            if (dep.getStatus() == 0) {
                FixedAsset asset = this.getById(dep.getAssetId());
                if (asset != null) {
                    asset.setAccumulatedDepreciation(asset.getAccumulatedDepreciation().add(dep.getPeriodDepreciation()));
                    asset.setNetValue(asset.getOriginalValue().subtract(asset.getAccumulatedDepreciation()));
                    asset.setUsedMonths(asset.getUsedMonths() + 1);
                    this.updateById(asset);
                }
                dep.setStatus(1);
                assetDepreciationService.updateById(dep);
            }
        }
        return true;
    }
    
    @Override
    public Map<String, BigDecimal> getAssetSummary(Long tenantId) {
        Map<String, BigDecimal> summary = new HashMap<>();
        summary.put("totalOriginalValue", baseMapper.sumOriginalValue(tenantId));
        summary.put("totalAccumulatedDepreciation", baseMapper.sumAccumulatedDepreciation(tenantId));
        summary.put("totalNetValue", baseMapper.sumNetValue(tenantId));
        Integer count = baseMapper.countActive(tenantId);
        summary.put("activeCount", BigDecimal.valueOf(count != null ? count : 0));
        return summary;
    }
    
    @Override
    public String generateAssetCode(Long tenantId) {
        String maxCode = baseMapper.getMaxAssetCode(tenantId);
        if (maxCode == null || maxCode.isEmpty()) {
            return "FA000001";
        }
        int nextNum = Integer.parseInt(maxCode.substring(2)) + 1;
        return "FA" + String.format("%06d", nextNum);
    }
}