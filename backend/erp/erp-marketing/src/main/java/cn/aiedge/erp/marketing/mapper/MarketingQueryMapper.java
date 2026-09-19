package cn.aiedge.erp.marketing.mapper;

import cn.aiedge.erp.marketing.dto.CouponRecordRow;
import cn.aiedge.erp.marketing.dto.PointsJournalRow;
import cn.aiedge.erp.marketing.dto.PresaleOrderRowVO;
import cn.aiedge.erp.marketing.dto.PromoteProductRow;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 营销域只读联表查询（跨表 join，手写 SQL）。
 *
 * <p>⚠️ 手写 SQL **不受多租户插件管辖**：每条语句都显式带 {@code tenant_id} 条件。</p>
 */
@Mapper
public interface MarketingQueryMapper {

    /** 优惠券「领用明细」（对标 13 列） */
    @Select("""
            <script>
            SELECT c.id,
                   p.party_name                                   AS partner_name,
                   pc.contact_name                                AS contact_name,
                   COALESCE(pc.mobile, pc.phone, p.phone)         AS contact_phone,
                   t.coupon_name                                  AS coupon_name,
                   t.coupon_type                                  AS coupon_type,
                   t.use_rule                                     AS use_rule,
                   COALESCE(c.face_value, t.face_value)           AS face_value,
                   c.status                                       AS raw_status,
                   so.order_no                                    AS bill_no,
                   t.status                                       AS status,
                   c.receive_time                                 AS receive_time,
                   c.used_time                                    AS used_time,
                   c.source_bill_no                               AS source_bill_no,
                   c.template_id                                  AS template_id,
                   c.partner_id                                   AS partner_id,
                   c.code                                         AS code
            FROM erp_loyalty_coupon c
            LEFT JOIN mkt_coupon_template t ON t.id = c.template_id AND t.deleted = 0
            LEFT JOIN biz_party p           ON p.id = c.partner_id  AND p.deleted = 0
            LEFT JOIN biz_party_contact pc  ON pc.party_id = p.id AND pc.deleted = 0 AND pc.is_primary = 1
            LEFT JOIN erp_sale_order so     ON so.id = c.used_order_id
            WHERE c.deleted = 0
              AND c.tenant_id = #{tenantId}
              <if test="templateId != null"> AND c.template_id = #{templateId} </if>
              <if test="status != null and status != ''"> AND c.status = #{status} </if>
              <if test="billNo != null and billNo != ''"> AND so.order_no LIKE CONCAT('%', #{billNo}, '%') </if>
              <if test="couponName != null and couponName != ''"> AND t.coupon_name LIKE CONCAT('%', #{couponName}, '%') </if>
              <if test="partnerId != null"> AND c.partner_id = #{partnerId} </if>
            ORDER BY c.receive_time DESC NULLS LAST, c.create_time DESC NULLS LAST, c.id DESC
            </script>
            """)
    IPage<CouponRecordRow> selectCouponRecordPage(Page<CouponRecordRow> page,
                                                  @Param("tenantId") Long tenantId,
                                                  @Param("templateId") Long templateId,
                                                  @Param("status") String status,
                                                  @Param("billNo") String billNo,
                                                  @Param("couponName") String couponName,
                                                  @Param("partnerId") Long partnerId);

    /** 按往来单位批量取「接收人 + 手机号」（营销短信群发用：优先主联系人手机，回退单位电话） */
    @Select("""
            <script>
            SELECT p.id                AS partner_id,
                   p.party_name        AS partner_name,
                   COALESCE(pc.mobile, pc.phone, p.phone) AS mobile
            FROM biz_party p
            LEFT JOIN biz_party_contact pc
                   ON pc.party_id = p.id AND pc.deleted = 0 AND pc.is_primary = 1
            WHERE p.deleted = 0
              AND p.tenant_id = #{tenantId}
              AND p.id IN
              <foreach collection="partnerIds" item="pid" open="(" separator="," close=")">#{pid}</foreach>
            </script>
            """)
    List<Map<String, Object>> selectPartnerContacts(@Param("tenantId") Long tenantId,
                                                    @Param("partnerIds") List<Long> partnerIds);

    /**
     * 「我要推广 → 商品」Tab（对标 17 列）：
     * 商品主数据 + 库存聚合（erp_stock）+ 最近销售时间（erp_sale_order）+ 分享统计（mkt_share_record）
     */
    @Select("""
            <script>
            SELECT p.id                        AS id,
                   p.image_url                 AS image_url,
                   p.product_code              AS product_code,
                   p.product_name              AS product_name,
                   p.brand                     AS brand,
                   p.spec                      AS spec,
                   p.model                     AS model,
                   p.origin                    AS origin,
                   p.unit                      AS unit,
                   COALESCE(st.stock, 0)       AS stock,
                   sale.last_sale_time         AS last_sale_time,
                   p.create_time               AS create_time,
                   pu.retail_price             AS retail_price,
                   pu.wholesale_price          AS wholesale_price,
                   sh.last_share_time          AS last_share_time,
                   COALESCE(sh.share_count, 0) AS share_count,
                   COALESCE(sh.viewer_count, 0) AS viewer_count,
                   COALESCE(sh.view_count, 0)  AS view_count
            FROM erp_product p
            LEFT JOIN (
                SELECT product_id, SUM(quantity) AS stock
                FROM erp_stock WHERE deleted = 0 GROUP BY product_id
            ) st ON st.product_id = p.id
            LEFT JOIN (
                SELECT soi.product_id, MAX(so.order_date) AS last_sale_time
                FROM erp_sale_order_item soi
                JOIN erp_sale_order so ON so.id = soi.order_id AND so.deleted = 0
                GROUP BY soi.product_id
            ) sale ON sale.product_id = p.id
            LEFT JOIN erp_product_unit pu
                   ON pu.product_id = p.id AND pu.deleted = 0 AND pu.is_base_unit = 1
            LEFT JOIN (
                SELECT target_id,
                       MAX(share_time) AS last_share_time,
                       COUNT(*)        AS share_count,
                       COALESCE(SUM(viewer_count), 0) AS viewer_count,
                       COALESCE(SUM(view_count), 0)   AS view_count
                FROM mkt_share_record
                WHERE deleted = 0 AND share_type = 'PRODUCT' AND tenant_id = #{tenantId}
                GROUP BY target_id
            ) sh ON sh.target_id = p.id
            WHERE p.deleted = 0
              AND p.tenant_id = #{tenantId}
              <if test="keyword != null and keyword != ''">
                AND (p.product_code LIKE CONCAT('%', #{keyword}, '%')
                     OR p.product_name LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            ORDER BY p.create_time DESC NULLS LAST, p.id DESC
            </script>
            """)
    IPage<PromoteProductRow> selectPromoteProductPage(Page<PromoteProductRow> page,
                                                       @Param("tenantId") Long tenantId,
                                                       @Param("keyword") String keyword);

    /** 商城预售 →「预售订单」Tab（8 列） */
    @Select("""
            <script>
            SELECT o.id                                AS id,
                   COALESCE(bp.party_name, '')         AS customer_name,
                   pre.product_name                    AS product_name,
                   so.order_no                         AS order_no,
                   so.order_date                       AS bill_date,
                   so.product_amount                   AS product_amount,
                   so.total_amount                     AS order_amount,
                   pre.activity_name                   AS activity_name,
                   CASE pre.status WHEN 1 THEN '进行中' WHEN 2 THEN '已结束' WHEN 3 THEN '已取消' ELSE '未开始' END AS activity_status,
                   o.presale_id                        AS presale_id,
                   o.mall_order_id                     AS order_id,
                   o.paid_deposit                      AS paid_deposit,
                   o.paid_final                        AS paid_final
            FROM mkt_presale_order o
            LEFT JOIN mkt_presale pre ON pre.id = o.presale_id
            LEFT JOIN biz_party bp    ON bp.id = o.customer_id
            LEFT JOIN erp_sale_order so ON so.id = o.mall_order_id
            WHERE o.deleted = 0
              AND o.tenant_id = #{tenantId}
              <if test="customer != null and customer != ''"> AND bp.party_name LIKE CONCAT('%', #{customer}, '%') </if>
              <if test="orderNo != null and orderNo != ''"> AND so.order_no LIKE CONCAT('%', #{orderNo}, '%') </if>
              <if test="presaleId != null"> AND o.presale_id = #{presaleId} </if>
            ORDER BY o.create_time DESC NULLS LAST, o.id DESC
            </script>
            """)
    IPage<PresaleOrderRowVO> selectPresaleOrderPage(Page<PresaleOrderRowVO> page,
                                                    @Param("tenantId") Long tenantId,
                                                    @Param("customer") String customer,
                                                    @Param("orderNo") String orderNo,
                                                    @Param("presaleId") Long presaleId);

    /** 会员积分明细（数据源：erp_sale_order_points_journal，与销售订单「会员信息」Tab 同源） */
    @Select("""
            <script>
            SELECT j.id,
                   so.order_no            AS order_no,
                   j.member_card_no       AS member_card_no,
                   j.member_name          AS member_name,
                   j.member_discount      AS member_discount,
                   j.prev_points          AS prev_points,
                   j.sale_points          AS sale_points,
                   j.return_points        AS return_points,
                   j.exchange_points      AS exchange_points,
                   j.used_points          AS used_points,
                   j.current_points       AS current_points,
                   j.create_time          AS create_time
            FROM erp_sale_order_points_journal j
            LEFT JOIN erp_sale_order so ON so.id = j.order_id
            WHERE j.tenant_id = #{tenantId}
              <if test="memberCardNo != null and memberCardNo != ''"> AND j.member_card_no = #{memberCardNo} </if>
              <if test="memberName != null and memberName != ''"> AND j.member_name LIKE CONCAT('%', #{memberName}, '%') </if>
            ORDER BY j.create_time DESC NULLS LAST, j.id DESC
            </script>
            """)
    IPage<PointsJournalRow> selectPointsJournalPage(Page<PointsJournalRow> page,
                                                    @Param("tenantId") Long tenantId,
                                                    @Param("memberCardNo") String memberCardNo,
                                                    @Param("memberName") String memberName);
}
