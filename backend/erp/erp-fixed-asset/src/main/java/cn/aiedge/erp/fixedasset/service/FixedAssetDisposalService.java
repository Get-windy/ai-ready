package cn.aiedge.erp.fixedasset.service;

import cn.aiedge.erp.fixedasset.dto.FixedAssetDisposalDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 固定资产处置服务接口
 */
public interface FixedAssetDisposalService {

    FixedAssetDisposalDTO create(FixedAssetDisposalDTO dto);

    FixedAssetDisposalDTO update(Long id, FixedAssetDisposalDTO dto);

    void delete(Long id);

    FixedAssetDisposalDTO getById(Long id);

    Page<FixedAssetDisposalDTO> getPage(String disposalNo, String status, Pageable pageable);

    FixedAssetDisposalDTO approve(Long id, String comment);

    FixedAssetDisposalDTO reject(Long id, String comment);
}
