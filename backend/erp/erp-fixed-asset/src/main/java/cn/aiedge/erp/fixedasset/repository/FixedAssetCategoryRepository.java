package cn.aiedge.erp.fixedasset.repository;

import cn.aiedge.erp.fixedasset.model.FixedAssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 固定资产分类仓库
 */
@Repository
public interface FixedAssetCategoryRepository extends JpaRepository<FixedAssetCategory, Long> {

    List<FixedAssetCategory> findByParentId(Long parentId);

    List<FixedAssetCategory> findByParentIdIsNull();
}
