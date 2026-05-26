package cn.aiedge.erp.supplier.mapper;

import cn.aiedge.erp.supplier.model.entity.SupplierLevelEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SupplierLevelMapper extends BaseMapper<SupplierLevelEntity> {
    
    @Select("SELECT * FROM erp_supplier_level WHERE tenant_id = #{tenantId} AND is_active = true AND deleted = 0 ORDER BY sort_order ASC")
    List<SupplierLevelEntity> findActiveLevels(@Param("tenantId") String tenantId);
    
    @Select("SELECT * FROM erp_supplier_level WHERE tenant_id = #{tenantId} AND min_score <= #{score} AND max_score >= #{score} AND is_active = true AND deleted = 0 ORDER BY priority_level DESC LIMIT 1")
    SupplierLevelEntity findLevelByScore(@Param("tenantId") String tenantId, @Param("score") Double score);
    
    @Select("SELECT * FROM erp_supplier_level WHERE tenant_id = #{tenantId} AND level_code = #{levelCode} AND deleted = 0")
    SupplierLevelEntity findByLevelCode(@Param("tenantId") String tenantId, @Param("levelCode") String levelCode);
}