package cn.aiedge.erp.expense.mapper;

import cn.aiedge.erp.expense.model.ExpensePayment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExpensePaymentMapper extends BaseMapper<ExpensePayment> {

    @Select("SELECT * FROM expense_payment WHERE application_id = #{applicationId} ORDER BY created_at DESC")
    List<ExpensePayment> findByApplicationId(@Param("applicationId") Long applicationId);

    @Select("SELECT * FROM expense_payment WHERE payer_id = #{payerId} ORDER BY created_at DESC")
    List<ExpensePayment> findByPayerId(@Param("payerId") String payerId);

    @Select("SELECT * FROM expense_payment WHERE payment_status = #{paymentStatus} ORDER BY created_at DESC")
    List<ExpensePayment> findByPaymentStatus(@Param("paymentStatus") String paymentStatus);
}
