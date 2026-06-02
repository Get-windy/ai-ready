package cn.aiedge.erp.fixedasset.service;

import cn.aiedge.erp.fixedasset.dto.FixedAssetCategoryDTO;

import java.util.List;
import java.util.Map;

/**
 * 固定资产分类服务接口
 */
public interface FixedAssetCategoryService {

    FixedAssetCategoryDTO create(FixedAssetCategoryDTO dto);

    FixedAssetCategoryDTO update(Long id, FixedAssetCategoryDTO dto);

    void delete(Long id);

    FixedAssetCategoryDTO getById(Long id);

    List<FixedAssetCategoryDTO> getAll();

    List<Map<String, Object>> getTree();
}
