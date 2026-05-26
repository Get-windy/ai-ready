package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.AssetChange;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AssetChangeService extends IService<AssetChange> {
    
    List<AssetChange> listByAssetId(Long tenantId, Long assetId);
    
    List<AssetChange> listByChangeType(Long tenantId, Integer changeType);
    
    Page<AssetChange> pageList(Long tenantId, Long assetId, Integer changeType, String startDate, String endDate, Page<AssetChange> page);
    
    boolean createChange(AssetChange change);
    
    boolean recordTransfer(Long tenantId, Long assetId, Long toDepartmentId, Long toLocationId, Long toCustodianId, String reason);
    
    boolean recordDispose(Long tenantId, Long assetId, String reason);
    
    boolean recordScrap(Long tenantId, Long assetId, String reason);
    
    boolean recordDepreciation(Long tenantId, Long assetId, BigDecimal depreciationAmount, String period);
}