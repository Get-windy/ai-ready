package cn.aiedge.erp.purchase.inbound.mapper;

import cn.aiedge.erp.purchase.inbound.entity.PurchaseInbound;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PurchaseInboundMapper extends BaseMapper<PurchaseInbound> {

    @Select("SELECT * FROM erp_purchase_inbound WHERE supplier_id = #{supplierId} AND deleted = 0 ORDER BY create_time DESC")
    List<PurchaseInbound> selectBySupplierId(@Param("supplierId") Long supplierId);

    @Select("SELECT * FROM erp_purchase_inbound WHERE order_id = #{orderId} AND deleted = 0 ORDER BY create_time DESC")
    List<PurchaseInbound> selectByOrderId(@Param("orderId") Long orderId);

    @Select("SELECT * FROM erp_purchase_inbound WHERE warehouse_id = #{warehouseId} AND deleted = 0 ORDER BY create_time DESC")
    List<PurchaseInbound> selectByWarehouseId(@Param("warehouseId") Long warehouseId);

    @Select("SELECT * FROM erp_purchase_inbound WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    List<PurchaseInbound> selectByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(*) FROM erp_purchase_inbound WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    Integer countByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT SUM(total_amount) FROM erp_purchase_inbound WHERE status = 9 AND deleted = 0 AND tenant_id = #{tenantId}")
    java.math.BigDecimal sumInboundAmount(@Param("tenantId") Long tenantId);
}