package cn.aiedge.erp.payment.mapper;

import cn.aiedge.erp.payment.entity.Payment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

    @Select("SELECT * FROM erp_payment WHERE supplier_id = #{supplierId} AND deleted = 0 ORDER BY create_time DESC")
    List<Payment> selectBySupplierId(@Param("supplierId") Long supplierId);

    @Select("SELECT * FROM erp_payment WHERE order_id = #{orderId} AND deleted = 0 ORDER BY create_time DESC")
    List<Payment> selectByOrderId(@Param("orderId") Long orderId);

    @Select("SELECT * FROM erp_payment WHERE invoice_id = #{invoiceId} AND deleted = 0 ORDER BY create_time DESC")
    List<Payment> selectByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Select("SELECT * FROM erp_payment WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    List<Payment> selectByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(*) FROM erp_payment WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    Integer countByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT SUM(payment_amount) FROM erp_payment WHERE status = 7 AND deleted = 0 AND tenant_id = #{tenantId}")
    java.math.BigDecimal sumPaymentAmount(@Param("tenantId") Long tenantId);

    @Select("SELECT SUM(verified_amount) FROM erp_payment WHERE status = 7 AND deleted = 0 AND tenant_id = #{tenantId}")
    java.math.BigDecimal sumVerifiedAmount(@Param("tenantId") Long tenantId);
}