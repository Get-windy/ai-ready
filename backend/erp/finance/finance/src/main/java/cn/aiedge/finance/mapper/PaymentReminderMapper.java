package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.PaymentReminder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface PaymentReminderMapper extends BaseMapper<PaymentReminder> {
    
    @Select("SELECT * FROM finance_payment_reminder WHERE tenant_id = #{tenantId} AND status = #{status} AND deleted = 0 ORDER BY overdue_days DESC")
    List<PaymentReminder> selectByStatus(@Param("tenantId") Long tenantId, @Param("status") Integer status);
    
    @Select("SELECT * FROM finance_payment_reminder WHERE tenant_id = #{tenantId} AND customer_id = #{customerId} AND deleted = 0 ORDER BY reminder_time DESC")
    List<PaymentReminder> selectByCustomerId(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId);
    
    @Select("SELECT * FROM finance_payment_reminder WHERE tenant_id = #{tenantId} AND receivable_id = #{receivableId} AND deleted = 0 ORDER BY reminder_time DESC")
    List<PaymentReminder> selectByReceivableId(@Param("tenantId") Long tenantId, @Param("receivableId") Long receivableId);
    
    @Select("SELECT * FROM finance_payment_reminder WHERE tenant_id = #{tenantId} AND next_reminder_time <= #{now} AND status IN (0, 1) AND deleted = 0")
    List<PaymentReminder> selectPendingReminder(@Param("tenantId") Long tenantId, @Param("now") LocalDateTime now);
    
    @Select("SELECT COUNT(*) FROM finance_payment_reminder WHERE tenant_id = #{tenantId} AND customer_id = #{customerId} AND status IN (0, 1) AND deleted = 0")
    Integer countPendingByCustomer(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId);
    
    @Select("SELECT SUM(overdue_amount) FROM finance_payment_reminder WHERE tenant_id = #{tenantId} AND status = #{status} AND deleted = 0")
    BigDecimal sumOverdueAmountByStatus(@Param("tenantId") Long tenantId, @Param("status") Integer status);
}