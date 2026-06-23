package cn.aiedge.erp.payment.mapper;

import cn.aiedge.erp.payment.entity.Receipt;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReceiptMapper extends BaseMapper<Receipt> {

    @Select("SELECT * FROM erp_receipt WHERE customer_id = #{customerId} AND deleted = 0 ORDER BY create_time DESC")
    List<Receipt> selectByCustomerId(@Param("customerId") Long customerId);

    @Select("SELECT * FROM erp_receipt WHERE order_id = #{orderId} AND deleted = 0 ORDER BY create_time DESC")
    List<Receipt> selectByOrderId(@Param("orderId") Long orderId);

    @Select("SELECT * FROM erp_receipt WHERE invoice_id = #{invoiceId} AND deleted = 0 ORDER BY create_time DESC")
    List<Receipt> selectByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Select("SELECT * FROM erp_receipt WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    List<Receipt> selectByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(*) FROM erp_receipt WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    Integer countByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT SUM(receipt_amount) FROM erp_receipt WHERE status = 7 AND deleted = 0 AND tenant_id = #{tenantId}")
    java.math.BigDecimal sumReceiptAmount(@Param("tenantId") Long tenantId);

    @Select("SELECT SUM(verified_amount) FROM erp_receipt WHERE status = 7 AND deleted = 0 AND tenant_id = #{tenantId}")
    java.math.BigDecimal sumVerifiedAmount(@Param("tenantId") Long tenantId);
}