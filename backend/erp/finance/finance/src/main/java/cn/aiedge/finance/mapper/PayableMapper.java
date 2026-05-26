package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.Payable;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface PayableMapper extends BaseMapper<Payable> {

    @Select("SELECT * FROM finance_payable WHERE supplier_id = #{supplierId} AND deleted = 0 ORDER BY due_date ASC")
    List<Payable> selectBySupplierId(@Param("supplierId") Long supplierId);

    @Select("SELECT * FROM finance_payable WHERE order_id = #{orderId} AND deleted = 0")
    List<Payable> selectByOrderId(@Param("orderId") Long orderId);

    @Select("SELECT * FROM finance_payable WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    List<Payable> selectByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT * FROM finance_payable WHERE due_date < CURRENT_DATE AND status IN (0, 1) AND deleted = 0")
    List<Payable> selectOverdue();

    @Select("SELECT SUM(original_amount) FROM finance_payable WHERE status IN (0, 1) AND deleted = 0 AND tenant_id = #{tenantId}")
    BigDecimal sumPendingAmount(@Param("tenantId") Long tenantId);

    @Select("SELECT SUM(overdue_amount) FROM finance_payable WHERE status = 3 AND deleted = 0 AND tenant_id = #{tenantId}")
    BigDecimal sumOverdueAmount(@Param("tenantId") Long tenantId);

    @Select("SELECT SUM(paid_amount) FROM finance_payable WHERE status = 2 AND deleted = 0 AND tenant_id = #{tenantId}")
    BigDecimal sumPaidAmount(@Param("tenantId") Long tenantId);
}