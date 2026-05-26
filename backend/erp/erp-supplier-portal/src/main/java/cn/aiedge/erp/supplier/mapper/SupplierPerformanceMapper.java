package cn.aiedge.erp.supplier.mapper;

import cn.aiedge.erp.supplier.model.entity.SupplierPerformanceEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SupplierPerformanceMapper extends BaseMapper<SupplierPerformanceEntity> {
    
    @Select("SELECT * FROM erp_supplier_performance WHERE tenant_id = #{tenantId} AND supplier_id = #{supplierId} AND deleted = 0 ORDER BY evaluation_date DESC")
    List<SupplierPerformanceEntity> findBySupplierId(@Param("tenantId") String tenantId, @Param("supplierId") Long supplierId);
    
    @Select("SELECT * FROM erp_supplier_performance WHERE tenant_id = #{tenantId} AND supplier_id = #{supplierId} AND evaluation_period = #{period} AND deleted = 0 ORDER BY evaluation_date DESC LIMIT 1")
    SupplierPerformanceEntity findLatestByPeriod(@Param("tenantId") String tenantId, @Param("supplierId") Long supplierId, @Param("period") String period);
    
    @Select("SELECT * FROM erp_supplier_performance WHERE tenant_id = #{tenantId} AND evaluation_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0")
    List<SupplierPerformanceEntity> findByDateRange(@Param("tenantId") String tenantId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Select("SELECT AVG(comprehensive_score) FROM erp_supplier_performance WHERE tenant_id = #{tenantId} AND supplier_id = #{supplierId} AND deleted = 0")
    Double getAverageScore(@Param("tenantId") String tenantId, @Param("supplierId") Long supplierId);
}