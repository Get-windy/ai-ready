package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.CostCenter;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CostCenterMapper extends BaseMapper<CostCenter> {
    
    @Select("SELECT * FROM finance_cost_center WHERE tenant_id = #{tenantId} AND deleted = 0 AND enabled = 1 ORDER BY center_code")
    List<CostCenter> listAllEnabled(@Param("tenantId") Long tenantId);
    
    @Select("SELECT * FROM finance_cost_center WHERE tenant_id = #{tenantId} AND deleted = 0 AND parent_id = #{parentId} ORDER BY center_code")
    List<CostCenter> listByParentId(@Param("tenantId") Long tenantId, @Param("parentId") Long parentId);
    
    @Select("SELECT * FROM finance_cost_center WHERE tenant_id = #{tenantId} AND deleted = 0 AND center_type = #{centerType} ORDER BY center_code")
    List<CostCenter> listByType(@Param("tenantId") Long tenantId, @Param("centerType") Integer centerType);
    
    @Select("SELECT * FROM finance_cost_center WHERE tenant_id = #{tenantId} AND deleted = 0 AND center_code = #{centerCode}")
    CostCenter getByCode(@Param("tenantId") Long tenantId, @Param("centerCode") String centerCode);
    
    @Select("SELECT COUNT(*) FROM finance_cost_center WHERE tenant_id = #{tenantId} AND deleted = 0 AND parent_id = #{parentId}")
    Integer countChildren(@Param("tenantId") Long tenantId, @Param("parentId") Long parentId);
}