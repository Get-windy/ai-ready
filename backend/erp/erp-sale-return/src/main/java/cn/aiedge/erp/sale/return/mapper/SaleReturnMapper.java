package cn.aiedge.erp.sale.return.mapper;

import cn.aiedge.erp.sale.return.entity.SaleReturn;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SaleReturnMapper extends BaseMapper<SaleReturn> {

    @Select("SELECT * FROM erp_sale_return WHERE customer_id = #{customerId} AND deleted = 0 ORDER BY create_time DESC")
    List<SaleReturn> selectByCustomerId(@Param("customerId") Long customerId);

    @Select("SELECT * FROM erp_sale_return WHERE order_id = #{orderId} AND deleted = 0 ORDER BY create_time DESC")
    List<SaleReturn> selectByOrderId(@Param("orderId") Long orderId);

    @Select("SELECT * FROM erp_sale_return WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    List<SaleReturn> selectByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(*) FROM erp_sale_return WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    Integer countByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT SUM(refund_amount) FROM erp_sale_return WHERE status = 11 AND deleted = 0 AND tenant_id = #{tenantId}")
    java.math.BigDecimal sumRefundAmount(@Param("tenantId") Long tenantId);
}