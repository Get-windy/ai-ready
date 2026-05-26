package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.AssetCategory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AssetCategoryMapper extends BaseMapper<AssetCategory> {
    
    @Select("SELECT * FROM finance_asset_category WHERE tenant_id = #{tenantId} AND deleted = 0 AND enabled = 1 ORDER BY category_code")
    List<AssetCategory> listAllEnabled(@Param("tenantId") Long tenantId);
    
    @Select("SELECT * FROM finance_asset_category WHERE tenant_id = #{tenantId} AND deleted = 0 AND parent_id = #{parentId} ORDER BY category_code")
    List<AssetCategory> listByParentId(@Param("tenantId") Long tenantId, @Param("parentId") Long parentId);
    
    @Select("SELECT * FROM finance_asset_category WHERE tenant_id = #{tenantId} AND deleted = 0 AND category_code = #{categoryCode}")
    AssetCategory getByCode(@Param("tenantId") Long tenantId, @Param("categoryCode") String categoryCode);
    
    @Select("SELECT COUNT(*) FROM finance_asset_category WHERE tenant_id = #{tenantId} AND deleted = 0 AND parent_id = #{parentId}")
    Integer countChildren(@Param("tenantId") Long tenantId, @Param("parentId") Long parentId);
}