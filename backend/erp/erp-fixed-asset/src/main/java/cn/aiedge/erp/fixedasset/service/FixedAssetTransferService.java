package cn.aiedge.erp.fixedasset.service;

import cn.aiedge.erp.fixedasset.dto.FixedAssetTransferDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 固定资产转移服务接口
 */
public interface FixedAssetTransferService {

    FixedAssetTransferDTO create(FixedAssetTransferDTO dto);

    FixedAssetTransferDTO update(Long id, FixedAssetTransferDTO dto);

    void delete(Long id);

    FixedAssetTransferDTO getById(Long id);

    Page<FixedAssetTransferDTO> getPage(String transferNo, String status, Pageable pageable);

    FixedAssetTransferDTO approve(Long id, String comment);

    FixedAssetTransferDTO reject(Long id, String comment);
}
