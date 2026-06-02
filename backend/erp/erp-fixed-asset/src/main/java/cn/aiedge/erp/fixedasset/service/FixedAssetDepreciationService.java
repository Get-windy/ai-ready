package cn.aiedge.erp.fixedasset.service;

import cn.aiedge.erp.fixedasset.dto.FixedAssetDepreciationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 固定资产折旧服务接口
 */
public interface FixedAssetDepreciationService {

    FixedAssetDepreciationDTO getById(Long id);

    Page<FixedAssetDepreciationDTO> getPage(Long assetId, String period, Pageable pageable);

    List<FixedAssetDepreciationDTO> batchCalculate();
}
