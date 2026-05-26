package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.AssetChange;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AssetChangeMapper extends BaseMapper<AssetChange> {
    
    @Select("SELECT * FROM finance_asset_change WHERE tenant_id = #{tenantId} AND deleted = 0 AND asset_id = #{assetId} ORDER BY change_date DESC")
    List<AssetChange> listByAssetId(@Param("tenantId") Long tenantId, @Param("assetId") Long assetId);
    
    @Select("SELECT * FROM finance_asset_change WHERE tenant_id = #{tenantId} AND deleted = 0 AND change_type = #{changeType} ORDER BY change_date DESC")
    List<AssetChange> listByChangeType(@Param("tenantId") Long tenantId, @Param("changeType") Integer changeType);
    
    @Select("SELECT * FROM finance_asset_change WHERE tenant_id = #{tenantId} AND deleted = 0 AND change_date BETWEEN #{startDate} AND #{endDate} ORDER BY change_date DESC")
    List<AssetChange> listByDateRange(@Param("tenantId") Long tenantId, @Param("startDate") String startDate, @Param("endDate") String endDate);
}