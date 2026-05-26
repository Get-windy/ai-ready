package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.FixedAsset;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface FixedAssetMapper extends BaseMapper<FixedAsset> {
    
    @Select("SELECT * FROM finance_fixed_asset WHERE tenant_id = #{tenantId} AND deleted = 0 ORDER BY asset_code")
    List<FixedAsset> listAll(@Param("tenantId") Long tenantId);
    
    @Select("SELECT * FROM finance_fixed_asset WHERE tenant_id = #{tenantId} AND deleted = 0 AND category_id = #{categoryId} ORDER BY asset_code")
    List<FixedAsset> listByCategoryId(@Param("tenantId") Long tenantId, @Param("categoryId") Long categoryId);
    
    @Select("SELECT * FROM finance_fixed_asset WHERE tenant_id = #{tenantId} AND deleted = 0 AND status = #{status} ORDER BY asset_code")
    List<FixedAsset> listByStatus(@Param("tenantId") Long tenantId, @Param("status") Integer status);
    
    @Select("SELECT * FROM finance_fixed_asset WHERE tenant_id = #{tenantId} AND deleted = 0 AND department_id = #{departmentId} ORDER BY asset_code")
    List<FixedAsset> listByDepartmentId(@Param("tenantId") Long tenantId, @Param("departmentId") Long departmentId);
    
    @Select("SELECT * FROM finance_fixed_asset WHERE tenant_id = #{tenantId} AND deleted = 0 AND asset_code = #{assetCode}")
    FixedAsset getByCode(@Param("tenantId") Long tenantId, @Param("assetCode") String assetCode);
    
    @Select("SELECT SUM(original_value) FROM finance_fixed_asset WHERE tenant_id = #{tenantId} AND deleted = 0 AND status IN (1, 2, 3)")
    BigDecimal sumOriginalValue(@Param("tenantId") Long tenantId);
    
    @Select("SELECT SUM(accumulated_depreciation) FROM finance_fixed_asset WHERE tenant_id = #{tenantId} AND deleted = 0 AND status IN (1, 2, 3)")
    BigDecimal sumAccumulatedDepreciation(@Param("tenantId") Long tenantId);
    
    @Select("SELECT SUM(net_value) FROM finance_fixed_asset WHERE tenant_id = #{tenantId} AND deleted = 0 AND status IN (1, 2, 3)")
    BigDecimal sumNetValue(@Param("tenantId") Long tenantId);
    
    @Select("SELECT COUNT(*) FROM finance_fixed_asset WHERE tenant_id = #{tenantId} AND deleted = 0 AND status IN (1, 2, 3)")
    Integer countActive(@Param("tenantId") Long tenantId);
    
    @Select("SELECT MAX(asset_code) FROM finance_fixed_asset WHERE tenant_id = #{tenantId}")
    String getMaxAssetCode(@Param("tenantId") Long tenantId);
}