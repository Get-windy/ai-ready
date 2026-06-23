package cn.aiedge.erp.payment.mapper;

import cn.aiedge.erp.payment.entity.PreReceipt;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PreReceiptMapper extends BaseMapper<PreReceipt> {

    @Select("SELECT * FROM erp_pre_receipt WHERE customer_id = #{customerId} AND deleted = 0 ORDER BY create_time DESC")
    List<PreReceipt> selectByCustomerId(@Param("customerId") Long customerId);

    @Select("SELECT * FROM erp_pre_receipt WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    List<PreReceipt> selectByStatus(@Param("status") String status, @Param("tenantId") Long tenantId);

    @Select("SELECT * FROM erp_pre_receipt WHERE pre_receipt_no = #{preReceiptNo} AND deleted = 0")
    PreReceipt selectByPreReceiptNo(@Param("preReceiptNo") String preReceiptNo);

    @Select("SELECT COALESCE(SUM(remaining_amount), 0) FROM erp_pre_receipt WHERE customer_id = #{customerId} AND deleted = 0 AND status IN ('received', 'offset')")
    java.math.BigDecimal sumRemainingByCustomer(@Param("customerId") Long customerId);
}
