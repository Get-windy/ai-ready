package cn.aiedge.erp.b2b.mapper;

import cn.aiedge.erp.b2b.dto.TradeAnalysisDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商城交易分析Mapper (真实SQL聚合)
 * 商城订单存储于 erp_sale_order, order_source=2 企业客户商城, order_source=3 个人会员商城
 */
@Mapper
public interface MallTradeAnalysisMapper {

    /**
     * 按日分组: 订单数/GMV/客单价 (排除已取消: 现口径status=6; status=5 为历史商城取消单兼容保留, 现口径5=交易完成)
     */
    @Select("""
            <script>
            SELECT to_char(CAST(o.order_date AS date), 'YYYY-MM-DD') AS day,
              COUNT(*) AS order_count,
              COALESCE(SUM(o.total_amount), 0) AS gmv,
              COALESCE(SUM(o.total_amount), 0) / NULLIF(COUNT(*), 0) AS avg_order_amount
            FROM erp_sale_order o
            WHERE o.deleted = 0
              AND o.order_source IN (2, 3)
              AND o.status NOT IN (5, 6)
            <if test="startTime != null">
              AND o.order_date &gt;= #{startTime}
            </if>
            <if test="endTime != null">
              AND o.order_date &lt;= #{endTime}
            </if>
            GROUP BY 1 ORDER BY 1
            </script>
            """)
    List<TradeAnalysisDTO.DailyItem> selectDaily(@Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 汇总: 总单数/GMV/客单价/退款单数/退款率
     * 退款口径: payment_status=4 已退款 (refund_request/refund_record 属支付中心, 与商城订单无关联字段)
     */
    @Select("""
            <script>
            SELECT
              COUNT(*) AS total_order_count,
              COALESCE(SUM(o.total_amount), 0) AS total_gmv,
              COALESCE(SUM(o.total_amount), 0) / NULLIF(COUNT(*), 0) AS avg_order_amount,
              SUM(CASE WHEN o.payment_status = 4 THEN 1 ELSE 0 END) AS refund_order_count,
              COALESCE(ROUND(SUM(CASE WHEN o.payment_status = 4 THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(*), 0), 2), 0) AS refund_rate
            FROM erp_sale_order o
            WHERE o.deleted = 0
              AND o.order_source IN (2, 3)
              AND o.status NOT IN (5, 6)
            <if test="startTime != null">
              AND o.order_date &gt;= #{startTime}
            </if>
            <if test="endTime != null">
              AND o.order_date &lt;= #{endTime}
            </if>
            </script>
            """)
    TradeAnalysisDTO.Summary selectSummary(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 支付状态分布 (含已取消订单, 反映全量支付状态)
     */
    @Select("""
            <script>
            SELECT o.payment_status, COUNT(*) AS count
            FROM erp_sale_order o
            WHERE o.deleted = 0
              AND o.order_source IN (2, 3)
            <if test="startTime != null">
              AND o.order_date &gt;= #{startTime}
            </if>
            <if test="endTime != null">
              AND o.order_date &lt;= #{endTime}
            </if>
            GROUP BY o.payment_status ORDER BY o.payment_status
            </script>
            """)
    List<TradeAnalysisDTO.PaymentStatusItem> selectPaymentStatusDistribution(@Param("startTime") LocalDateTime startTime,
                                                                             @Param("endTime") LocalDateTime endTime);
}
