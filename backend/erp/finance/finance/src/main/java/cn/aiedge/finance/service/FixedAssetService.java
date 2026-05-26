package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.FixedAsset;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface FixedAssetService extends IService<FixedAsset> {
    
    List<FixedAsset> listAll(Long tenantId);
    
    List<FixedAsset> listByCategoryId(Long tenantId, Long categoryId);
    
    List<FixedAsset> listByStatus(Long tenantId, Integer status);
    
    List<FixedAsset> listByDepartmentId(Long tenantId, Long departmentId);
    
    FixedAsset getByCode(Long tenantId, String assetCode);
    
    Page<FixedAsset> pageList(Long tenantId, String assetCode, String assetName, Long categoryId, Integer status, Long departmentId, Page<FixedAsset> page);
    
    boolean createAsset(FixedAsset asset);
    
    boolean updateAsset(FixedAsset asset);
    
    boolean deleteAsset(Long tenantId, Long assetId);
    
    boolean transfer(Long tenantId, Long assetId, Long toDepartmentId, Long toLocationId, Long toCustodianId, String reason);
    
    boolean dispose(Long tenantId, Long assetId, BigDecimal disposeValue, String reason);
    
    boolean scrap(Long tenantId, Long assetId, String reason);
    
    BigDecimal calculateDepreciation(FixedAsset asset);
    
    boolean runDepreciation(Long tenantId, String period);
    
    boolean postDepreciation(Long tenantId, String period);
    
    Map<String, BigDecimal> getAssetSummary(Long tenantId);
    
    String generateAssetCode(Long tenantId);
}