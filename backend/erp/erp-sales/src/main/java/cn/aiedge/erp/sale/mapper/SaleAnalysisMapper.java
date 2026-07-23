package cn.aiedge.erp.sale.mapper;

import cn.aiedge.erp.sale.dto.CustomerActiveAnalysisDTO;
import cn.aiedge.erp.sale.dto.PromotionAnalysisDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 销售分析Mapper (真实SQL聚合)
 */
@Mapper
public interface SaleAnalysisMapper {

    /**
     * 客户活跃分析分页
     * biz_party_follow 已由 V11.16.0 迁移补齐 tenant_id 列，全局租户拦截器可直接生效
     */
    @Select("""
            <script>
            SELECT
              o.customer_id,
              MAX(o.customer_name) AS customer_name,
              SUM(CASE WHEN o.order_date >= #{recentSince} THEN 1 ELSE 0 END) AS recent_order_count,
              COALESCE(SUM(CASE WHEN o.order_date >= #{recentSince} THEN o.total_amount ELSE 0 END), 0) AS recent_order_amount,
              MAX(o.order_date) AS last_order_time,
              COUNT(*) AS total_order_count,
              COALESCE(SUM(o.total_amount), 0) AS total_order_amount,
              COALESCE(f.follow_count, 0) AS follow_count,
              CASE
                WHEN MAX(o.order_date) >= #{activeSince} THEN '活跃'
                WHEN MAX(o.order_date) >= #{normalSince} THEN '一般'
                ELSE '沉默'
              END AS activity_level
            FROM erp_sale_order o
            LEFT JOIN (
              SELECT party_id, COUNT(*) AS follow_count
              FROM biz_party_follow WHERE deleted = 0 GROUP BY party_id
            ) f ON f.party_id = o.customer_id
            WHERE o.deleted = 0
              AND o.status &lt;&gt; 6
              AND NOT (o.order_source IN (2, 3) AND o.status = 5)
            <if test="tenantId != null">
              AND o.tenant_id = #{tenantId}
            </if>
            <if test="keyword != null and keyword != ''">
              AND o.customer_name ILIKE CONCAT('%', #{keyword}, '%')
            </if>
            GROUP BY o.customer_id, f.follow_count
            ORDER BY recent_order_count DESC, total_order_amount DESC
            </script>
            """)
    IPage<CustomerActiveAnalysisDTO> selectCustomerActivePage(Page<CustomerActiveAnalysisDTO> page,
                                                              @Param("tenantId") Long tenantId,
                                                              @Param("keyword") String keyword,
                                                              @Param("recentSince") LocalDateTime recentSince,
                                                              @Param("activeSince") LocalDateTime activeSince,
                                                              @Param("normalSince") LocalDateTime normalSince);

    /**
     * 促销活动按状态计数
     */
    @Select("SELECT status, COUNT(*) AS count FROM erp_promotion_activity WHERE deleted = 0 GROUP BY status")
    List<Map<String, Object>> selectPromotionCountByStatus();

    /**
     * 促销活动按类型计数
     */
    @Select("SELECT type, COUNT(*) AS count FROM erp_promotion_activity WHERE deleted = 0 GROUP BY type")
    List<Map<String, Object>> selectPromotionCountByType();

    /**
     * 促销活动按月时间分布 (按 start_time 归月)
     */
    @Select("""
            <script>
            SELECT to_char(date_trunc('month', start_time), 'YYYY-MM') AS month, COUNT(*) AS count
            FROM erp_promotion_activity
            WHERE deleted = 0
            <if test="startTime != null">
              AND start_time &gt;= #{startTime}
            </if>
            <if test="endTime != null">
              AND start_time &lt;= #{endTime}
            </if>
            GROUP BY 1 ORDER BY 1
            </script>
            """)
    List<Map<String, Object>> selectPromotionMonthlyDistribution(@Param("startTime") LocalDateTime startTime,
                                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 查询与区间重叠的促销活动列表 (区间为空时返回全部)
     */
    @Select("""
            <script>
            SELECT id, name, type, status, start_time, end_time, discount_rate, reduction_amount, create_time
            FROM erp_promotion_activity
            WHERE deleted = 0
            <if test="startTime != null">
              AND end_time &gt;= #{startTime}
            </if>
            <if test="endTime != null">
              AND start_time &lt;= #{endTime}
            </if>
            ORDER BY start_time DESC
            </script>
            """)
    List<PromotionAnalysisDTO.ActivityItem> selectPromotionActivities(@Param("startTime") LocalDateTime startTime,
                                                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 区间订单优惠概况 (全量口径, 订单无 promotion_id 无法归因到具体活动)
     * 优惠金额 = promo_discount + coupon_amount + direct_discount + discount_amount
     */
    @Select("""
            <script>
            SELECT
              COUNT(*) AS order_count,
              COALESCE(SUM(total_amount), 0) AS total_order_amount,
              SUM(CASE WHEN COALESCE(promo_discount, 0) + COALESCE(coupon_amount, 0)
                          + COALESCE(direct_discount, 0) + COALESCE(discount_amount, 0) &gt; 0
                       THEN 1 ELSE 0 END) AS discounted_order_count,
              COALESCE(SUM(COALESCE(promo_discount, 0) + COALESCE(coupon_amount, 0)
                          + COALESCE(direct_discount, 0) + COALESCE(discount_amount, 0)), 0) AS total_discount_amount
            FROM erp_sale_order
            WHERE deleted = 0
              AND status &lt;&gt; 6
              AND NOT (order_source IN (2, 3) AND status = 5)
            <if test="startTime != null">
              AND order_date &gt;= #{startTime}
            </if>
            <if test="endTime != null">
              AND order_date &lt;= #{endTime}
            </if>
            </script>
            """)
    PromotionAnalysisDTO.DiscountOverview selectOrderDiscountOverview(@Param("startTime") LocalDateTime startTime,
                                                                      @Param("endTime") LocalDateTime endTime);
}
