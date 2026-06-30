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

    /**
     * 查询今日销售总额（关联明细表按金额汇总）
     */
    @Select("SELECT COALESCE(SUM(i.amount), 0) FROM erp_sale_order o " +
            "INNER JOIN erp_sale_order_item i ON o.id = i.order_id " +
            "WHERE o.tenant_id = #{tenantId} AND o.create_time::date = CURRENT_DATE AND o.deleted = 0")
    java.math.BigDecimal sumTodaySalesAmount(@Param("tenantId") Long tenantId);

    /**
     * 查询本月销售总额
     */
    @Select("SELECT COALESCE(SUM(i.amount), 0) FROM erp_sale_order o " +
            "INNER JOIN erp_sale_order_item i ON o.id = i.order_id " +
            "WHERE o.tenant_id = #{tenantId} " +
            "AND EXTRACT(YEAR FROM o.create_time) = EXTRACT(YEAR FROM CURRENT_DATE) " +
            "AND EXTRACT(MONTH FROM o.create_time) = EXTRACT(MONTH FROM CURRENT_DATE) " +
            "AND o.deleted = 0")
    java.math.BigDecimal sumMonthSalesAmount(@Param("tenantId") Long tenantId);

    /**
     * 查询指定日期范围的每日销售趋势
     */
    @Select("SELECT DATE(o.create_time) as day, COALESCE(SUM(i.amount), 0) as amount " +
            "FROM erp_sale_order o INNER JOIN erp_sale_order_item i ON o.id = i.order_id " +
            "WHERE o.tenant_id = #{tenantId} AND o.create_time >= #{startDate} AND o.create_time < #{endDate} AND o.deleted = 0 " +
            "GROUP BY DATE(o.create_time) ORDER BY day")
    List<java.util.Map<String, Object>> selectDailySalesTrend(@Param("tenantId") Long tenantId,
                                                               @Param("startDate") String startDate,
                                                               @Param("endDate") String endDate);
}