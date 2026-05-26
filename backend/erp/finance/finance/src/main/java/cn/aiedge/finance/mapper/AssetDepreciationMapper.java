package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.AssetDepreciation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface AssetDepreciationMapper extends BaseMapper<AssetDepreciation> {
    
    @Select("SELECT * FROM finance_asset_depreciation WHERE tenant_id = #{tenantId} AND deleted = 0 AND period = #{period} ORDER BY asset_code")
    List<AssetDepreciation> listByPeriod(@Param("tenantId") Long tenantId, @Param("period") String period);
    
    @Select("SELECT * FROM finance_asset_depreciation WHERE tenant_id = #{tenantId} AND deleted = 0 AND asset_id = #{assetId} ORDER BY period DESC")
    List<AssetDepreciation> listByAssetId(@Param("tenantId") Long tenantId, @Param("assetId") Long assetId);
    
    @Select("SELECT * FROM finance_asset_depreciation WHERE tenant_id = #{tenantId} AND deleted = 0 AND asset_id = #{assetId} AND period = #{period}")
    AssetDepreciation getByAssetIdAndPeriod(@Param("tenantId") Long tenantId, @Param("assetId") Long assetId, @Param("period") String period);
    
    @Select("SELECT SUM(period_depreciation) FROM finance_asset_depreciation WHERE tenant_id = #{tenantId} AND deleted = 0 AND period = #{period}")
    BigDecimal sumPeriodDepreciation(@Param("tenantId") Long tenantId, @Param("period") String period);
    
    @Select("SELECT COUNT(*) FROM finance_asset_depreciation WHERE tenant_id = #{tenantId} AND deleted = 0 AND period = #{period} AND status = 0")
    Integer countUnpostedByPeriod(@Param("tenantId") Long tenantId, @Param("period") String period);
}