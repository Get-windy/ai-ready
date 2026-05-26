package cn.aiedge.erp.supplier.mapper;

import cn.aiedge.erp.supplier.model.entity.SupplierPointsRecordEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SupplierPointsRecordMapper extends BaseMapper<SupplierPointsRecordEntity> {
    
    @Select("SELECT * FROM erp_supplier_points_record WHERE tenant_id = #{tenantId} AND supplier_id = #{supplierId} AND deleted = 0 ORDER BY create_time DESC")
    List<SupplierPointsRecordEntity> findBySupplierId(@Param("tenantId") String tenantId, @Param("supplierId") Long supplierId);
    
    @Select("SELECT SUM(points) FROM erp_supplier_points_record WHERE tenant_id = #{tenantId} AND supplier_id = #{supplierId} AND points_type = 1 AND deleted = 0")
    Integer getTotalEarnedPoints(@Param("tenantId") String tenantId, @Param("supplierId") Long supplierId);
    
    @Select("SELECT SUM(points) FROM erp_supplier_points_record WHERE tenant_id = #{tenantId} AND supplier_id = #{supplierId} AND points_type = 2 AND deleted = 0")
    Integer getTotalConsumedPoints(@Param("tenantId") String tenantId, @Param("supplierId") Long supplierId);
}