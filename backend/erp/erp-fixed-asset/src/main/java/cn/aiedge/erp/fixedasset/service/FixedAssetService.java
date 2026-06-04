package cn.aiedge.erp.fixedasset.service;

import cn.aiedge.erp.fixedasset.dto.FixedAssetDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * 固定资产服务接口
 */
public interface FixedAssetService {

    FixedAssetDTO create(FixedAssetDTO dto);

    FixedAssetDTO update(Long id, FixedAssetDTO dto);

    void delete(Long id);

    FixedAssetDTO getById(Long id);

    Page<FixedAssetDTO> getPage(String assetCode, String assetName, Long categoryId, String status,
                                String departmentId, String keyword, Pageable pageable);

    FixedAssetDTO depreciate(Long id);

    Map<String, Object> getStatistics();

    void batchDelete(List<Long> ids);

    List<FixedAssetDTO> exportList(String assetCode, String assetName, Long categoryId, String status,
                                    String departmentId, String keyword);
}
