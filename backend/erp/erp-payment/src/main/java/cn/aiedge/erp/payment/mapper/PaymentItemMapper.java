package cn.aiedge.erp.payment.mapper;

import cn.aiedge.erp.payment.entity.PaymentItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PaymentItemMapper extends BaseMapper<PaymentItem> {

    @Select("SELECT * FROM erp_payment_item WHERE payment_id = #{paymentId} AND deleted = 0 ORDER BY line_no ASC")
    List<PaymentItem> selectByPaymentId(@Param("paymentId") Long paymentId);

    @Select("SELECT SUM(verify_amount) FROM erp_payment_item WHERE payment_id = #{paymentId} AND deleted = 0")
    java.math.BigDecimal sumVerifyAmountByPaymentId(@Param("paymentId") Long paymentId);

    @Select("SELECT SUM(verified_amount) FROM erp_payment_item WHERE payment_id = #{paymentId} AND deleted = 0")
    java.math.BigDecimal sumVerifiedAmountByPaymentId(@Param("paymentId") Long paymentId);
}