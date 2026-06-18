package cn.aiedge.erp.product.kit.mapper;

import cn.aiedge.erp.product.kit.entity.KitAssembly;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KitAssemblyMapper extends BaseMapper<KitAssembly> {

    @Select("SELECT * FROM erp_kit_assembly WHERE kit_id = #{kitId} AND deleted = 0 ORDER BY create_time DESC")
    List<KitAssembly> selectByKitId(@Param("kitId") Long kitId);

    @Select("SELECT * FROM erp_kit_assembly WHERE warehouse_id = #{warehouseId} AND deleted = 0 ORDER BY create_time DESC")
    List<KitAssembly> selectByWarehouseId(@Param("warehouseId") Long warehouseId);

    @Select("SELECT * FROM erp_kit_assembly WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    List<KitAssembly> selectByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(*) FROM erp_kit_assembly WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    Integer countByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT SUM(total_cost) FROM erp_kit_assembly WHERE status = 6 AND deleted = 0 AND tenant_id = #{tenantId}")
    java.math.BigDecimal sumAssemblyCost(@Param("tenantId") Long tenantId);
}