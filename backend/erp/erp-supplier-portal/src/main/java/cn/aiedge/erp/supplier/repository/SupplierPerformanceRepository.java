package cn.aiedge.erp.supplier.repository;

import cn.aiedge.erp.supplier.model.entity.SupplierPerformanceEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 供应商绩效评估数据访问层
 */
@Mapper
public interface SupplierPerformanceRepository extends BaseMapper<SupplierPerformanceEntity> {
    
    /**
     * 根据供应商ID查询绩效评估列表
     * 
     * @param supplierId 供应商ID
     * @param tenantId 租户ID
     * @return 绩效评估列表
     */
    @Select("SELECT * FROM erp_supplier_performance WHERE supplier_id = #{supplierId} AND tenant_id = #{tenantId} AND deleted = 0 ORDER BY evaluation_date DESC")
    List<SupplierPerformanceEntity> selectBySupplierId(@Param("supplierId") Long supplierId, @Param("tenantId") String tenantId);
    
    /**
     * 根据供应商ID和评估周期查询绩效评估
     * 
     * @param supplierId 供应商ID
     * @param evaluationPeriod 评估周期
     * @param evaluationType 评估类型
     * @param tenantId 租户ID
     * @return 绩效评估实体
     */
    @Select("SELECT * FROM erp_supplier_performance WHERE supplier_id = #{supplierId} AND evaluation_period = #{evaluationPeriod} AND evaluation_type = #{evaluationType} AND tenant_id = #{tenantId} AND deleted = 0")
    SupplierPerformanceEntity selectBySupplierIdAndPeriod(@Param("supplierId") Long supplierId, @Param("evaluationPeriod") String evaluationPeriod, @Param("evaluationType") Integer evaluationType, @Param("tenantId") String tenantId);
    
    /**
     * 查询供应商最新绩效评估
     * 
     * @param supplierId 供应商ID
     * @param tenantId 租户ID
     * @return 绩效评估实体
     */
    @Select("SELECT * FROM erp_supplier_performance WHERE supplier_id = #{supplierId} AND tenant_id = #{tenantId} AND deleted = 0 ORDER BY evaluation_date DESC LIMIT 1")
    SupplierPerformanceEntity selectLatestBySupplierId(@Param("supplierId") Long supplierId, @Param("tenantId") String tenantId);
}