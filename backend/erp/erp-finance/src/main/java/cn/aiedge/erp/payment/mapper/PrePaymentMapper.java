package cn.aiedge.erp.payment.mapper;

import cn.aiedge.erp.payment.entity.PrePayment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PrePaymentMapper extends BaseMapper<PrePayment> {

    @Select("SELECT * FROM erp_pre_payment WHERE supplier_id = #{supplierId} AND deleted = 0 ORDER BY create_time DESC")
    List<PrePayment> selectBySupplierId(@Param("supplierId") Long supplierId);

    @Select("SELECT * FROM erp_pre_payment WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    List<PrePayment> selectByStatus(@Param("status") String status, @Param("tenantId") Long tenantId);

    @Select("SELECT * FROM erp_pre_payment WHERE pre_payment_no = #{prePaymentNo} AND deleted = 0")
    PrePayment selectByPrePaymentNo(@Param("prePaymentNo") String prePaymentNo);

    @Select("SELECT COALESCE(SUM(remaining_amount), 0) FROM erp_pre_payment WHERE supplier_id = #{supplierId} AND deleted = 0 AND status IN ('paid', 'offset')")
    java.math.BigDecimal sumRemainingBySupplier(@Param("supplierId") Long supplierId);
}
