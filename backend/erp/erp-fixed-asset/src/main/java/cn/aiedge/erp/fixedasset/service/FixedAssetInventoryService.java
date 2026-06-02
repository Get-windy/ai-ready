package cn.aiedge.erp.fixedasset.service;

import cn.aiedge.erp.fixedasset.dto.FixedAssetInventoryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 固定资产盘点服务接口
 */
public interface FixedAssetInventoryService {

    FixedAssetInventoryDTO create(FixedAssetInventoryDTO dto);

    FixedAssetInventoryDTO update(Long id, FixedAssetInventoryDTO dto);

    FixedAssetInventoryDTO getById(Long id);

    Page<FixedAssetInventoryDTO> getPage(String inventoryNo, String status, String checkResult, Pageable pageable);
}
