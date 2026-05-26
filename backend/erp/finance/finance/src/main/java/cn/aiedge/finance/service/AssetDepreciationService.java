package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.AssetDepreciation;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface AssetDepreciationService extends IService<AssetDepreciation> {
    
    List<AssetDepreciation> listByPeriod(Long tenantId, String period);
    
    List<AssetDepreciation> listByAssetId(Long tenantId, Long assetId);
    
    AssetDepreciation getByAssetIdAndPeriod(Long tenantId, Long assetId, String period);
    
    Page<AssetDepreciation> pageList(Long tenantId, String period, String assetCode, Integer status, Page<AssetDepreciation> page);
    
    boolean createDepreciation(AssetDepreciation depreciation);
    
    boolean postDepreciation(Long tenantId, Long depreciationId);
    
    boolean batchPostDepreciation(Long tenantId, String period);
    
    BigDecimal sumPeriodDepreciation(Long tenantId, String period);
    
    Map<String, Object> getDepreciationSummary(Long tenantId, String period);
}