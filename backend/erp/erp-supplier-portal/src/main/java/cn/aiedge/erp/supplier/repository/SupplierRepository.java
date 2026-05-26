package cn.aiedge.erp.supplier.repository;

import cn.aiedge.erp.supplier.model.entity.SupplierEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 供应商数据访问层
 */
@Mapper
public interface SupplierRepository extends BaseMapper<SupplierEntity> {
    
    /**
     * 根据租户ID和供应商编码查询供应商
     * 
     * @param tenantId 租户ID
     * @param supplierCode 供应商编码
     * @return 供应商实体
     */
    @Select("SELECT * FROM erp_supplier WHERE tenant_id = #{tenantId} AND supplier_code = #{supplierCode} AND deleted = 0")
    SupplierEntity selectByTenantIdAndSupplierCode(@Param("tenantId") String tenantId, @Param("supplierCode") String supplierCode);
    
    /**
     * 根据租户ID查询供应商列表
     * 
     * @param tenantId 租户ID
     * @return 供应商列表
     */
    @Select("SELECT * FROM erp_supplier WHERE tenant_id = #{tenantId} AND deleted = 0 ORDER BY create_time DESC")
    List<SupplierEntity> selectByTenantId(@Param("tenantId") String tenantId);
    
    /**
     * 统计供应商数量
     * 
     * @param tenantId 租户ID
     * @return 供应商数量
     */
    @Select("SELECT COUNT(*) FROM erp_supplier WHERE tenant_id = #{tenantId} AND deleted = 0")
    Long countByTenantId(@Param("tenantId") String tenantId);
    
    /**
     * 根据合作状态统计供应商数量
     * 
     * @param tenantId 租户ID
     * @param cooperationStatus 合作状态
     * @return 供应商数量
     */
    @Select("SELECT COUNT(*) FROM erp_supplier WHERE tenant_id = #{tenantId} AND cooperation_status = #{cooperationStatus} AND deleted = 0")
    Long countByTenantIdAndCooperationStatus(@Param("tenantId") String tenantId, @Param("cooperationStatus") Integer cooperationStatus);
    
    /**
     * 根据等级统计供应商数量
     * 
     * @param tenantId 租户ID
     * @param supplierLevel 供应商等级
     * @return 供应商数量
     */
    @Select("SELECT COUNT(*) FROM erp_supplier WHERE tenant_id = #{tenantId} AND supplier_level = #{supplierLevel} AND deleted = 0")
    Long countByTenantIdAndSupplierLevel(@Param("tenantId") String tenantId, @Param("supplierLevel") String supplierLevel);
}