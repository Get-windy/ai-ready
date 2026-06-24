package cn.aiedge.erp.sale.mapper;

import cn.aiedge.erp.sale.entity.SaleOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 销售订单Mapper
 */
@Mapper
public interface SaleOrderMapper extends BaseMapper<SaleOrder> {

    /**
     * 根据订单号查询
     */
    @Select("SELECT * FROM erp_sale_order WHERE order_no = #{orderNo} AND tenant_id = #{tenantId}")
    SaleOrder selectByOrderNo(@Param("orderNo") String orderNo, @Param("tenantId") Long tenantId);

    /**
     * 更新订单状态
     */
    @Update("UPDATE erp_sale_order SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 更新收款金额
     */
    @Update("UPDATE erp_sale_order SET received_amount = received_amount + #{amount}, update_time = NOW() WHERE id = #{id}")
    int addReceivedAmount(@Param("id") Long id, @Param("amount") java.math.BigDecimal amount);

    /**
     * 查询待审批订单
     */
    @Select("SELECT * FROM erp_sale_order WHERE tenant_id = #{tenantId} AND status = 1 ORDER BY create_time DESC")
    List<SaleOrder> selectPendingOrders(@Param("tenantId") Long tenantId);

    /**
     * 查询客户订单
     */
    @Select("SELECT * FROM erp_sale_order WHERE customer_id = #{customerId} ORDER BY order_date DESC")
    List<SaleOrder> selectByCustomerId(@Param("customerId") Long customerId);

    /**
     * 查询今日新增订单数量（PostgreSQL语法）
     */
    @Select("SELECT COUNT(*) FROM erp_sale_order WHERE tenant_id = #{tenantId} AND create_time::date = CURRENT_DATE AND deleted = 0")
    int countTodayOrders(@Param("tenantId") Long tenantId);

    /**
     * 查询本月新增订单数量（PostgreSQL语法）
     */
    @Select("SELECT COUNT(*) FROM erp_sale_order WHERE tenant_id = #{tenantId} AND EXTRACT(YEAR FROM create_time) = EXTRACT(YEAR FROM CURRENT_DATE) AND EXTRACT(MONTH FROM create_time) = EXTRACT(MONTH FROM CURRENT_DATE) AND deleted = 0")
    int countMonthOrders(@Param("tenantId") Long tenantId);

    /**
     * 查询待审核订单数量（状态=1）
     */
    @Select("SELECT COUNT(*) FROM erp_sale_order WHERE tenant_id = #{tenantId} AND status = 1 AND deleted = 0")
    int countPendingApproval(@Param("tenantId") Long tenantId);

    /**
     * 查询待处理订单数量（状态=2 已审批待出库 或 状态=3 部分出库）
     */
    @Select("SELECT COUNT(*) FROM erp_sale_order WHERE tenant_id = #{tenantId} AND status IN (2, 3) AND deleted = 0")
    int countPendingProcess(@Param("tenantId") Long tenantId);
}