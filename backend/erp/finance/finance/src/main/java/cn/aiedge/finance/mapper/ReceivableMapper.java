package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.Receivable;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ReceivableMapper extends BaseMapper<Receivable> {

    @Select("SELECT * FROM finance_receivable WHERE customer_id = #{customerId} AND deleted = 0 ORDER BY due_date ASC")
    List<Receivable> selectByCustomerId(@Param("customerId") Long customerId);

    @Select("SELECT * FROM finance_receivable WHERE order_id = #{orderId} AND deleted = 0")
    List<Receivable> selectByOrderId(@Param("orderId") Long orderId);

    @Select("SELECT * FROM finance_receivable WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    List<Receivable> selectByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT * FROM finance_receivable WHERE due_date < CURRENT_DATE AND status IN (0, 1) AND deleted = 0")
    List<Receivable> selectOverdue();

    @Select("SELECT SUM(original_amount) FROM finance_receivable WHERE status IN (0, 1) AND deleted = 0 AND tenant_id = #{tenantId}")
    BigDecimal sumPendingAmount(@Param("tenantId") Long tenantId);

    @Select("SELECT SUM(overdue_amount) FROM finance_receivable WHERE status = 3 AND deleted = 0 AND tenant_id = #{tenantId}")
    BigDecimal sumOverdueAmount(@Param("tenantId") Long tenantId);

    @Select("SELECT SUM(received_amount) FROM finance_receivable WHERE status = 2 AND deleted = 0 AND tenant_id = #{tenantId}")
    BigDecimal sumReceivedAmount(@Param("tenantId") Long tenantId);
}