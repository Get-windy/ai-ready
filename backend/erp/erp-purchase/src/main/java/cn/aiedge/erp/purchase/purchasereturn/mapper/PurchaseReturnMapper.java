package cn.aiedge.erp.purchase.purchasereturn.mapper;

import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturn;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PurchaseReturnMapper extends BaseMapper<PurchaseReturn> {

    @Select("SELECT * FROM erp_purchase_return WHERE supplier_id = #{supplierId} AND deleted = 0 ORDER BY create_time DESC")
    List<PurchaseReturn> selectBySupplierId(@Param("supplierId") Long supplierId);

    @Select("SELECT * FROM erp_purchase_return WHERE purchase_order_id = #{orderId} AND deleted = 0 ORDER BY create_time DESC")
    List<PurchaseReturn> selectByOrderId(@Param("orderId") Long orderId);

    @Select("SELECT COUNT(*) FROM erp_purchase_return WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    Integer countByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT SUM(total_amount_with_tax) FROM erp_purchase_return WHERE status = 4 AND deleted = 0 AND tenant_id = #{tenantId}")
    java.math.BigDecimal sumReturnAmount(@Param("tenantId") Long tenantId);
}
