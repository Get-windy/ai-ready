package cn.aiedge.erp.sale.outbound.mapper;

import cn.aiedge.erp.sale.outbound.entity.SaleOutbound;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SaleOutboundMapper extends BaseMapper<SaleOutbound> {

    @Select("SELECT * FROM erp_sale_outbound WHERE customer_id = #{customerId} AND deleted = 0 ORDER BY create_time DESC")
    List<SaleOutbound> selectByCustomerId(@Param("customerId") Long customerId);

    @Select("SELECT * FROM erp_sale_outbound WHERE order_id = #{orderId} AND deleted = 0 ORDER BY create_time DESC")
    List<SaleOutbound> selectByOrderId(@Param("orderId") Long orderId);

    @Select("SELECT * FROM erp_sale_outbound WHERE warehouse_id = #{warehouseId} AND deleted = 0 ORDER BY create_time DESC")
    List<SaleOutbound> selectByWarehouseId(@Param("warehouseId") Long warehouseId);

    @Select("SELECT * FROM erp_sale_outbound WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    List<SaleOutbound> selectByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(*) FROM erp_sale_outbound WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    Integer countByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT SUM(total_amount) FROM erp_sale_outbound WHERE status = 11 AND deleted = 0 AND tenant_id = #{tenantId}")
    java.math.BigDecimal sumOutboundAmount(@Param("tenantId") Long tenantId);
}