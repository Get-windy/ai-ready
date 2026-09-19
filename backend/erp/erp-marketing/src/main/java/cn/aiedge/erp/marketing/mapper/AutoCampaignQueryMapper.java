package cn.aiedge.erp.marketing.mapper;

import cn.aiedge.erp.marketing.dto.AutoCampaignCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 营销自动化候选会员查询（按触发点各一条 SQL）。
 *
 * <p>⚠️ 手写 SQL 不受多租户插件管辖：每条都显式带 {@code tenant_id}。</p>
 * <p>会员口径与《会员管理》一致：`biz_party` 中 party_type=1 且存在会员卡号或会员名称。</p>
 */
@Mapper
public interface AutoCampaignQueryMapper {

    /**
     * 新客首单后 N 天内。
     *
     * <p>⚠️ 首单时间<b>不在 biz_party 上</b>（该表无 first_order_time 列），必须从销售单真实推导：
     * 按 {@code erp_sale_order.customer_id} 求 MIN(order_date)。原实现引用不存在的列，
     * 一旦启用「新客首单」触发点即整条 SQL 报 UndefinedColumn。</p>
     */
    @Select("""
            <script>
            SELECT p.id AS partner_id, p.party_code, p.party_name, p.member_name, p.member_card_no,
                   COALESCE(pc.mobile, pc.phone, p.phone) AS mobile,
                   CONCAT('首单于 ', to_char(f.first_order_date, 'YYYY-MM-DD'), ' 下单') AS trigger_note,
                   to_char(f.first_order_date, 'YYYY-MM-DD') AS key_date
            FROM (
                SELECT so.customer_id AS partner_id, MIN(so.order_date)::date AS first_order_date
                FROM erp_sale_order so
                WHERE so.deleted = 0 AND so.tenant_id = #{tenantId} AND so.customer_id IS NOT NULL
                GROUP BY so.customer_id
            ) f
            JOIN biz_party p ON p.id = f.partner_id
                 AND p.deleted = 0 AND p.tenant_id = #{tenantId} AND p.party_type = 1
            LEFT JOIN biz_party_contact pc ON pc.party_id = p.id AND pc.deleted = 0 AND pc.is_primary = 1
            WHERE f.first_order_date >= CURRENT_DATE - CAST(#{days} AS INTEGER)
            ORDER BY f.first_order_date DESC, p.id
            LIMIT #{limit}
            </script>
            """)
    List<AutoCampaignCandidate> selectNewCustomer(@Param("tenantId") Long tenantId,
                                                  @Param("days") Integer days,
                                                  @Param("limit") Integer limit);

    /** 会员生日前 N 天内（按 月-日 比较，跨年可用） */
    @Select("""
            <script>
            SELECT p.id AS partner_id, p.party_code, p.party_name, p.member_name, p.member_card_no,
                   COALESCE(pc.mobile, pc.phone, p.phone) AS mobile,
                   CONCAT('生日还有 ', EXTRACT(DAY FROM (date_trunc('day', p.birthday_this_year) - date_trunc('day', now())))::int, ' 天') AS trigger_note,
                   to_char(p.birthday_this_year, 'YYYY-MM-DD') AS key_date
            FROM (
                SELECT bp.*,
                       -- 今年生日：把生日平移到今年（已过则顺延到明年），从而支持跨年判断
                       (bp.birthday
                          + make_interval(years => (EXTRACT(YEAR FROM now()) - EXTRACT(YEAR FROM bp.birthday))::int)
                          + CASE WHEN (bp.birthday + make_interval(years => (EXTRACT(YEAR FROM now()) - EXTRACT(YEAR FROM bp.birthday))::int))::date
                                      &lt; CURRENT_DATE
                                 THEN interval '1 year' ELSE interval '0' END
                       )::date AS birthday_this_year
                FROM biz_party bp
                WHERE bp.deleted = 0 AND bp.tenant_id = #{tenantId} AND bp.party_type = 1
                  AND bp.birthday IS NOT NULL
            ) p
            LEFT JOIN biz_party_contact pc ON pc.party_id = p.id AND pc.deleted = 0 AND pc.is_primary = 1
            WHERE p.deleted = 0
              AND p.birthday_this_year >= date_trunc('day', now())
              AND p.birthday_this_year &lt;= date_trunc('day', now()) + (#{days} || ' days')::interval
            ORDER BY p.birthday_this_year
            LIMIT #{limit}
            </script>
            """)
    List<AutoCampaignCandidate> selectBirthday(@Param("tenantId") Long tenantId,
                                               @Param("days") Integer days,
                                               @Param("limit") Integer limit);

    /** 沉睡：最近交易早于 N 天前（且有过交易） */
    @Select("""
            <script>
            SELECT p.id AS partner_id, p.party_code, p.party_name, p.member_name, p.member_card_no,
                   COALESCE(pc.mobile, pc.phone, p.phone) AS mobile,
                   CONCAT('已沉睡 ', GREATEST(EXTRACT(DAY FROM (date_trunc('day', now()) - date_trunc('day', p.last_trade_time)))::int, 0), ' 天') AS trigger_note,
                   to_char(p.last_trade_time, 'YYYY-MM-DD') AS key_date
            FROM biz_party p
            LEFT JOIN biz_party_contact pc ON pc.party_id = p.id AND pc.deleted = 0 AND pc.is_primary = 1
            WHERE p.deleted = 0 AND p.tenant_id = #{tenantId} AND p.party_type = 1
              AND p.last_trade_time IS NOT NULL
              AND p.last_trade_time &lt;= now() - (#{days} || ' days')::interval
            ORDER BY p.last_trade_time
            LIMIT #{limit}
            </script>
            """)
    List<AutoCampaignCandidate> selectSleeping(@Param("tenantId") Long tenantId,
                                               @Param("days") Integer days,
                                               @Param("limit") Integer limit);

    /** 复购周期到期：距上次消费恰好落在 [N-3, N] 天窗口内（按周期提醒，避免与沉睡重叠） */
    @Select("""
            <script>
            SELECT p.id AS partner_id, p.party_code, p.party_name, p.member_name, p.member_card_no,
                   COALESCE(pc.mobile, pc.phone, p.phone) AS mobile,
                   CONCAT('距上次消费 ', GREATEST(EXTRACT(DAY FROM (date_trunc('day', now()) - date_trunc('day', p.last_trade_time)))::int, 0), ' 天，已到复购周期') AS trigger_note,
                   to_char(p.last_trade_time, 'YYYY-MM-DD') AS key_date
            FROM biz_party p
            LEFT JOIN biz_party_contact pc ON pc.party_id = p.id AND pc.deleted = 0 AND pc.is_primary = 1
            WHERE p.deleted = 0 AND p.tenant_id = #{tenantId} AND p.party_type = 1
              AND p.last_trade_time IS NOT NULL
              AND p.last_trade_time &lt;= now() - (#{days} || ' days')::interval
              AND p.last_trade_time >  now() - ((#{days} + 3) || ' days')::interval
            ORDER BY p.last_trade_time
            LIMIT #{limit}
            </script>
            """)
    List<AutoCampaignCandidate> selectRepurchase(@Param("tenantId") Long tenantId,
                                                 @Param("days") Integer days,
                                                 @Param("limit") Integer limit);

    /** 按会员卡号补会员基础信息（供积分到期候选展示与触达） */
    @Select("""
            SELECT p.id, p.party_code, p.party_name, p.member_name, p.member_card_no,
                   COALESCE(pc.mobile, pc.phone, p.phone) AS mobile
            FROM biz_party p
            LEFT JOIN biz_party_contact pc ON pc.party_id = p.id AND pc.deleted = 0 AND pc.is_primary = 1
            WHERE p.deleted = 0 AND p.tenant_id = #{tenantId} AND p.member_card_no = #{memberCardNo}
            LIMIT 1
            """)
    Map<String, Object> selectMemberByCard(@Param("memberCardNo") String memberCardNo,
                                           @Param("tenantId") Long tenantId);

    /** 会员卡到期前 N 天内 */
    @Select("""
            <script>
            SELECT p.id AS partner_id, p.party_code, p.party_name, p.member_name, p.member_card_no,
                   COALESCE(pc.mobile, pc.phone, p.phone) AS mobile,
                   CONCAT('会员卡还有 ', GREATEST(EXTRACT(DAY FROM (date_trunc('day', p.member_valid_end) - date_trunc('day', now())))::int, 0), ' 天到期') AS trigger_note,
                   to_char(p.member_valid_end, 'YYYY-MM-DD') AS key_date
            FROM biz_party p
            LEFT JOIN biz_party_contact pc ON pc.party_id = p.id AND pc.deleted = 0 AND pc.is_primary = 1
            WHERE p.deleted = 0 AND p.tenant_id = #{tenantId} AND p.party_type = 1
              AND p.member_valid_end IS NOT NULL
              AND p.member_valid_end >= date_trunc('day', now())
              AND p.member_valid_end &lt;= date_trunc('day', now()) + (#{days} || ' days')::interval
            ORDER BY p.member_valid_end
            LIMIT #{limit}
            </script>
            """)
    List<AutoCampaignCandidate> selectCardExpiring(@Param("tenantId") Long tenantId,
                                                   @Param("days") Integer days,
                                                   @Param("limit") Integer limit);
}
