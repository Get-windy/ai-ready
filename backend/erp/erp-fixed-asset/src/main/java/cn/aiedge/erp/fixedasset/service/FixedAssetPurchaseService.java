package cn.aiedge.erp.fixedasset.service;

import cn.aiedge.erp.fixedasset.dto.FixedAssetPurchaseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * 固定资产购置申请服务接口
 */
public interface FixedAssetPurchaseService {

    FixedAssetPurchaseDTO create(FixedAssetPurchaseDTO dto);

    FixedAssetPurchaseDTO update(Long id, FixedAssetPurchaseDTO dto);

    void delete(Long id);

    FixedAssetPurchaseDTO getById(Long id);

    Page<FixedAssetPurchaseDTO> getPage(String purchaseNo, String status, String title, Pageable pageable);

    FixedAssetPurchaseDTO submit(Long id);

    FixedAssetPurchaseDTO approve(Long id, String comment);

    FixedAssetPurchaseDTO reject(Long id, String comment);

    FixedAssetPurchaseDTO accept(Long id, FixedAssetPurchaseDTO dto);

    Map<String, Object> getStatistics();
}
