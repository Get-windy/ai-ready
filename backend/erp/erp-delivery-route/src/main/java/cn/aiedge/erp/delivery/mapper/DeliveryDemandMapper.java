package cn.aiedge.erp.delivery.mapper;

import cn.aiedge.erp.delivery.dto.DeliveryDemandVO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 配送需求只读查询（来源单据 → 可入线的配送需求）
 *
 * ⚠️ 多租户（重要）：客户档案 `biz_party` 存在 **tenant_id = 0 的全局数据**（如「散客（零售默认）」），
 * 而销售出库单/订单会引用这类全局客户。若交给多租户插件统一注入，JOIN 到 biz_party 的行会被过滤掉，
 * 导致「客户地址/坐标读不到」。
 * 故本 Mapper **显式关闭租户插件**（`@InterceptorIgnore`，与 `DmsConfigMapper` 同口径），
 * 并自行按「业务表严格等于当前租户 + 客户表允许当前租户或全局(0)」的口径过滤，避免越权。
 *
 * 边界：本模块**只读**销售订单/销售出库单与客户档案，不修改销售侧数据（配送不改库存）。
 * 「已入线」判定：`erp_route_point.order_id` 以 `OUT:{id}` / `SO:{id}` 为来源单据命名键。
 */
@Mapper
public interface DeliveryDemandMapper {

    /** 销售出库单：已发货（status=10）待配送 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT 'OUT' AS source_type, o.id AS source_id, o.outbound_no AS bill_no,
                   o.customer_id, o.customer_name,
                   COALESCE(NULLIF(o.shipping_address, ''), p.address) AS address,
                   COALESCE(NULLIF(o.receiver_name, ''), o.contact_name) AS receiver_name,
                   o.receiver_phone AS receiver_phone,
                   p.latitude AS latitude, p.longitude AS longitude,
                   o.create_time AS bill_time
              FROM erp_sale_outbound o
              LEFT JOIN biz_party p ON p.id = o.customer_id AND p.deleted = 0
                   AND (p.tenant_id = #{tenantId} OR p.tenant_id = 0)
             WHERE o.deleted = 0 AND o.tenant_id = #{tenantId} AND o.status = 10
               AND NOT EXISTS (SELECT 1 FROM erp_route_point pt
                                 JOIN erp_delivery_route r ON r.id = pt.route_id AND r.deleted = 0
                                 AND r.tenant_id = #{tenantId}
                                WHERE pt.order_id = 'OUT:' || o.id
                                  AND pt.tenant_id = #{tenantId}
                                  AND r.status IN ('PLANNING', 'READY', 'IN_PROGRESS'))
               AND (#{keyword,jdbcType=VARCHAR} IS NULL
                    OR o.outbound_no ILIKE '%' || #{keyword,jdbcType=VARCHAR} || '%'
                    OR o.customer_name ILIKE '%' || #{keyword,jdbcType=VARCHAR} || '%')
             ORDER BY o.create_time ASC, o.id ASC
             LIMIT #{limit}
            """)
    List<DeliveryDemandVO> selectOutboundDemands(@Param("tenantId") Long tenantId,
                                                 @Param("keyword") String keyword,
                                                 @Param("limit") int limit);

    /** 销售订单：待发货（2）/ 部分发货（3） */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT 'SO' AS source_type, s.id AS source_id, s.order_no AS bill_no,
                   s.customer_id, s.customer_name,
                   COALESCE(NULLIF(s.shipping_address, ''), NULLIF(s.consignee_address, ''), p.address) AS address,
                   COALESCE(NULLIF(s.receiver_name, ''), NULLIF(s.consignee, ''), s.contact_name) AS receiver_name,
                   COALESCE(NULLIF(s.receiver_phone, ''), s.consignee_phone, s.contact_phone) AS receiver_phone,
                   p.latitude AS latitude, p.longitude AS longitude,
                   s.create_time AS bill_time
              FROM erp_sale_order s
              LEFT JOIN biz_party p ON p.id = s.customer_id AND p.deleted = 0
                   AND (p.tenant_id = #{tenantId} OR p.tenant_id = 0)
             WHERE s.deleted = 0 AND s.tenant_id = #{tenantId} AND s.status IN (2, 3)
               AND NOT EXISTS (SELECT 1 FROM erp_route_point pt
                                 JOIN erp_delivery_route r ON r.id = pt.route_id AND r.deleted = 0
                                 AND r.tenant_id = #{tenantId}
                                WHERE pt.order_id = 'SO:' || s.id
                                  AND pt.tenant_id = #{tenantId}
                                  AND r.status IN ('PLANNING', 'READY', 'IN_PROGRESS'))
               AND (#{keyword,jdbcType=VARCHAR} IS NULL
                    OR s.order_no ILIKE '%' || #{keyword,jdbcType=VARCHAR} || '%'
                    OR s.customer_name ILIKE '%' || #{keyword,jdbcType=VARCHAR} || '%')
             ORDER BY s.create_time ASC, s.id ASC
             LIMIT #{limit}
            """)
    List<DeliveryDemandVO> selectOrderDemands(@Param("tenantId") Long tenantId,
                                              @Param("keyword") String keyword,
                                              @Param("limit") int limit);

    /** 按来源单据取单条配送需求（手动添加点位用；不限状态，允许把围栏外单据人工补进） */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT 'OUT' AS source_type, o.id AS source_id, o.outbound_no AS bill_no,
                   o.customer_id, o.customer_name,
                   COALESCE(NULLIF(o.shipping_address, ''), p.address) AS address,
                   COALESCE(NULLIF(o.receiver_name, ''), o.contact_name) AS receiver_name,
                   o.receiver_phone AS receiver_phone,
                   p.latitude AS latitude, p.longitude AS longitude,
                   o.create_time AS bill_time
              FROM erp_sale_outbound o
              LEFT JOIN biz_party p ON p.id = o.customer_id AND p.deleted = 0
                   AND (p.tenant_id = #{tenantId} OR p.tenant_id = 0)
             WHERE o.deleted = 0 AND o.tenant_id = #{tenantId} AND o.id = #{sourceId}
            """)
    DeliveryDemandVO selectOutboundDemand(@Param("tenantId") Long tenantId, @Param("sourceId") Long sourceId);

    /** 按来源单据取单条配送需求（销售订单） */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT 'SO' AS source_type, s.id AS source_id, s.order_no AS bill_no,
                   s.customer_id, s.customer_name,
                   COALESCE(NULLIF(s.shipping_address, ''), NULLIF(s.consignee_address, ''), p.address) AS address,
                   COALESCE(NULLIF(s.receiver_name, ''), NULLIF(s.consignee, ''), s.contact_name) AS receiver_name,
                   COALESCE(NULLIF(s.receiver_phone, ''), s.consignee_phone, s.contact_phone) AS receiver_phone,
                   p.latitude AS latitude, p.longitude AS longitude,
                   s.create_time AS bill_time
              FROM erp_sale_order s
              LEFT JOIN biz_party p ON p.id = s.customer_id AND p.deleted = 0
                   AND (p.tenant_id = #{tenantId} OR p.tenant_id = 0)
             WHERE s.deleted = 0 AND s.tenant_id = #{tenantId} AND s.id = #{sourceId}
            """)
    DeliveryDemandVO selectOrderDemand(@Param("tenantId") Long tenantId, @Param("sourceId") Long sourceId);
}
