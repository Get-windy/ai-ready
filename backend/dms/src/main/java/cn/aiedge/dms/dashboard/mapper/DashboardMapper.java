package cn.aiedge.dms.dashboard.mapper;

import cn.aiedge.dms.dashboard.dto.CapacityAggVO;
import cn.aiedge.dms.dashboard.dto.DistributionItemVO;
import cn.aiedge.dms.dashboard.dto.TaskAggVO;
import cn.aiedge.dms.dashboard.dto.TaskSummaryItemVO;
import cn.aiedge.dms.dashboard.dto.TopRiderVO;
import cn.aiedge.dms.dashboard.dto.TrendPointVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 配送仪表盘聚合查询 Mapper
 *
 * 说明：
 * 1. 全部为只读聚合，不写库；{@code @TableLogic} 逻辑删除对自定义 SQL 不生效，故每条 SQL 显式带 deleted = 0；
 * 2. 多租户条件显式写 tenant_id = #{tenantId}（inSql/apply 类内嵌 SQL 不会被多租户插件注入），
 *    插件对主表再注入一次同值条件无副作用；
 * 3. 渠道/订单类型为可选筛选，用 &lt;script&gt; 动态 SQL 在参数为空时整段省略，避免 PostgreSQL 参数类型推断问题；
 * 4. 口径固化见《配送仪表盘开发文档》§3.2，禁止在页面侧二次汇总。
 *
 * @author AI-Ready Team
 */
@Mapper
public interface DashboardMapper {

    /** 配送任务区间聚合（单量结构 / 准时率分子分母 / 时长 / 金额） */
    @Select("<script>" + """
            SELECT
                count(*) AS order_count,
                count(*) FILTER (WHERE status = 0) AS pending_orders,
                count(*) FILTER (WHERE status IN (1, 2)) AS assigned_orders,
                count(*) FILTER (WHERE status IN (3, 4)) AS in_transit_orders,
                count(*) FILTER (WHERE status IN (5, 6)) AS completed_orders,
                count(*) FILTER (WHERE status = 7) AS cancelled_orders,
                count(*) FILTER (WHERE status = 8) AS exception_orders,
                count(*) FILTER (WHERE status IN (0, 1, 2, 3, 4) AND deadline_time IS NOT NULL
                                   AND deadline_time &lt; now()) AS overdue_orders,
                count(*) FILTER (WHERE status IN (5, 6) AND deadline_time IS NOT NULL
                                   AND completed_time IS NOT NULL AND completed_time &lt;= deadline_time) AS on_time_count,
                count(*) FILTER (WHERE status IN (5, 6) AND deadline_time IS NOT NULL
                                   AND completed_time IS NOT NULL) AS on_time_base,
                avg(EXTRACT(EPOCH FROM (completed_time - pickup_time)) / 60.0)
                    FILTER (WHERE status IN (5, 6) AND completed_time IS NOT NULL AND pickup_time IS NOT NULL)
                    AS avg_delivery_minutes,
                COALESCE(sum(delivery_fee), 0) AS delivery_fee,
                COALESCE(sum(collect_on_delivery), 0) AS collect_on_delivery,
                COALESCE(sum(goods_amount), 0) AS goods_amount
            FROM dms_task
            WHERE deleted = 0
              AND tenant_id = #{tenantId}
              AND create_time &gt;= #{start}
              AND create_time &lt; #{end}
              <if test="channelId != null">AND channel_id = #{channelId}</if>
              <if test="orderType != null">AND order_type = #{orderType}</if>
            </script>
            """)
    TaskAggVO aggregateTask(@Param("tenantId") Long tenantId,
                            @Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end,
                            @Param("channelId") Long channelId,
                            @Param("orderType") Integer orderType);

    /** 配送员运力聚合：总数 / 活跃（空闲、忙碌） / 在线（阈值内有上报） */
    @Select("""
            SELECT
                count(*) AS total_riders,
                count(*) FILTER (WHERE status IN (1, 2)) AS active_riders,
                count(*) FILTER (WHERE last_report_time IS NOT NULL AND last_report_time >= #{onlineSince}) AS online_riders
            FROM dms_rider
            WHERE deleted = 0 AND tenant_id = #{tenantId}
            """)
    CapacityAggVO aggregateRider(@Param("tenantId") Long tenantId,
                                 @Param("onlineSince") LocalDateTime onlineSince);

    /** 车辆运力聚合：总数 / 活跃（使用中、已出勤） */
    @Select("""
            SELECT
                count(*) AS total_vehicles,
                count(*) FILTER (WHERE status IN (1, 4)) AS active_vehicles
            FROM dms_vehicle
            WHERE deleted = 0 AND tenant_id = #{tenantId}
            """)
    CapacityAggVO aggregateVehicle(@Param("tenantId") Long tenantId);

    /** 任务状态分布 */
    @Select("<script>" + """
            SELECT status AS status, count(*) AS count
            FROM dms_task
            WHERE deleted = 0
              AND tenant_id = #{tenantId}
              AND create_time &gt;= #{start}
              AND create_time &lt; #{end}
              <if test="channelId != null">AND channel_id = #{channelId}</if>
              <if test="orderType != null">AND order_type = #{orderType}</if>
            GROUP BY status
            ORDER BY status
            </script>
            """)
    List<TaskSummaryItemVO> selectStatusDistribution(@Param("tenantId") Long tenantId,
                                                     @Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end,
                                                     @Param("channelId") Long channelId,
                                                     @Param("orderType") Integer orderType);

    /** 按日趋势（单量 / 完成量 / 准时量 / 平均时长 / 配送费） */
    @Select("<script>" + """
            SELECT
                to_char(create_time::date, 'YYYY-MM-DD') AS stat_date,
                count(*) AS order_count,
                count(*) FILTER (WHERE status IN (5, 6)) AS completed_count,
                count(*) FILTER (WHERE status IN (5, 6) AND deadline_time IS NOT NULL
                                   AND completed_time IS NOT NULL AND completed_time &lt;= deadline_time) AS on_time_count,
                count(*) FILTER (WHERE status IN (5, 6) AND deadline_time IS NOT NULL
                                   AND completed_time IS NOT NULL) AS on_time_base,
                round((avg(EXTRACT(EPOCH FROM (completed_time - pickup_time)) / 60.0)
                    FILTER (WHERE status IN (5, 6) AND completed_time IS NOT NULL AND pickup_time IS NOT NULL))::numeric, 1)
                    AS avg_minutes,
                COALESCE(sum(delivery_fee), 0) AS delivery_fee
            FROM dms_task
            WHERE deleted = 0
              AND tenant_id = #{tenantId}
              AND create_time &gt;= #{start}
              AND create_time &lt; #{end}
              <if test="channelId != null">AND channel_id = #{channelId}</if>
              <if test="orderType != null">AND order_type = #{orderType}</if>
            GROUP BY create_time::date
            ORDER BY create_time::date
            </script>
            """)
    List<TrendPointVO> selectTrend(@Param("tenantId") Long tenantId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end,
                                   @Param("channelId") Long channelId,
                                   @Param("orderType") Integer orderType);

    /** 配送员绩效 Top（仅统计区间内有单的配送员，按接单量倒序、完成量次之） */
    @Select("<script>" + """
            SELECT
                r.id AS rider_id,
                r.real_name AS rider_name,
                r.phone AS phone,
                r.rating_score AS rating_score,
                count(t.id) AS order_count,
                count(t.id) FILTER (WHERE t.status IN (5, 6)) AS completed_count,
                count(t.id) FILTER (WHERE t.status IN (5, 6) AND t.deadline_time IS NOT NULL
                                     AND t.completed_time IS NOT NULL AND t.completed_time &lt;= t.deadline_time) AS on_time_count,
                count(t.id) FILTER (WHERE t.status IN (5, 6) AND t.deadline_time IS NOT NULL
                                     AND t.completed_time IS NOT NULL) AS on_time_base,
                round((avg(EXTRACT(EPOCH FROM (t.completed_time - t.pickup_time)) / 60.0)
                    FILTER (WHERE t.status IN (5, 6) AND t.completed_time IS NOT NULL AND t.pickup_time IS NOT NULL))::numeric, 1)
                    AS avg_minutes
            FROM dms_rider r
            LEFT JOIN dms_task t
                   ON t.rider_id = r.id
                  AND t.deleted = 0
                  AND t.tenant_id = #{tenantId}
                  AND t.create_time &gt;= #{start}
                  AND t.create_time &lt; #{end}
                  <if test="channelId != null">AND t.channel_id = #{channelId}</if>
                  <if test="orderType != null">AND t.order_type = #{orderType}</if>
            WHERE r.deleted = 0 AND r.tenant_id = #{tenantId}
            GROUP BY r.id, r.real_name, r.phone, r.rating_score
            HAVING count(t.id) &gt; 0
            ORDER BY order_count DESC, completed_count DESC, r.id
            LIMIT #{limit}
            </script>
            """)
    List<TopRiderVO> selectTopRiders(@Param("tenantId") Long tenantId,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end,
                                     @Param("channelId") Long channelId,
                                     @Param("orderType") Integer orderType,
                                     @Param("limit") int limit);

    /** 按渠道分布（未指派渠道归入「未分配渠道」） */
    @Select("<script>" + """
            SELECT
                COALESCE(t.channel_id, -1)::text AS item_key,
                COALESCE(c.channel_name, '未分配渠道') AS item_name,
                count(*) AS order_count,
                COALESCE(sum(t.delivery_fee), 0) AS amount
            FROM dms_task t
            LEFT JOIN dms_channel c
                   ON c.id = t.channel_id
                  AND c.deleted = 0
                  AND c.tenant_id = #{tenantId}
            WHERE t.deleted = 0
              AND t.tenant_id = #{tenantId}
              AND t.create_time &gt;= #{start}
              AND t.create_time &lt; #{end}
              <if test="channelId != null">AND t.channel_id = #{channelId}</if>
              <if test="orderType != null">AND t.order_type = #{orderType}</if>
            GROUP BY COALESCE(t.channel_id, -1), c.channel_name
            ORDER BY order_count DESC
            </script>
            """)
    List<DistributionItemVO> selectChannelDistribution(@Param("tenantId") Long tenantId,
                                                       @Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end,
                                                       @Param("channelId") Long channelId,
                                                       @Param("orderType") Integer orderType);

    /** 按订单类型分布（1-销售配送 2-调拨 3-退货，文案在 Service 侧用字典映射；未填类型归入「未指定」） */
    @Select("<script>" + """
            SELECT
                COALESCE(t.order_type, -1)::text AS item_key,
                count(*) AS order_count,
                COALESCE(sum(t.delivery_fee), 0) AS amount
            FROM dms_task t
            WHERE t.deleted = 0
              AND t.tenant_id = #{tenantId}
              AND t.create_time &gt;= #{start}
              AND t.create_time &lt; #{end}
              <if test="channelId != null">AND t.channel_id = #{channelId}</if>
              <if test="orderType != null">AND t.order_type = #{orderType}</if>
            GROUP BY COALESCE(t.order_type, -1)
            ORDER BY order_count DESC
            </script>
            """)
    List<DistributionItemVO> selectOrderTypeDistribution(@Param("tenantId") Long tenantId,
                                                         @Param("start") LocalDateTime start,
                                                         @Param("end") LocalDateTime end,
                                                         @Param("channelId") Long channelId,
                                                         @Param("orderType") Integer orderType);

    /** 活跃人车绑定数 */
    @Select("""
            SELECT count(*) FROM dms_rider_vehicle_binding
            WHERE deleted = 0 AND tenant_id = #{tenantId} AND status = 0
            """)
    Long countActiveBindings(@Param("tenantId") Long tenantId);

    /** 待处理核验预警数（handle_status = 0 未处理） */
    @Select("""
            SELECT count(*) FROM dms_verification_alert
            WHERE deleted = 0 AND tenant_id = #{tenantId} AND handle_status = 0
            """)
    Long countPendingAlerts(@Param("tenantId") Long tenantId);

    /** 读取租户级配送参数值（不存在返回 null，由 Service 回落到默认值） */
    @Select("""
            SELECT config_value FROM dms_config
            WHERE deleted = 0 AND tenant_id = #{tenantId} AND config_key = #{key}
            LIMIT 1
            """)
    String selectConfigValue(@Param("tenantId") Long tenantId, @Param("key") String key);
}
