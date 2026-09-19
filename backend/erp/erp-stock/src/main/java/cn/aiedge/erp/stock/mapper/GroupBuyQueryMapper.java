package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.dto.GroupBuyActivityRowVO;
import cn.aiedge.erp.stock.dto.GroupBuyOrderRowVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 商城拼团双 Tab 只读查询（拼团活动 / 拼团订单）——手写 SQL，显式带 tenant_id */
@Mapper
public interface GroupBuyQueryMapper {

    /** 「拼团活动」Tab：9 列，「开团 / 成功团」个数由参与记录聚合 */
    @Select("""
            <script>
            SELECT a.id                                AS id,
                   COALESCE(NULLIF(a.activity_code, ''), a.id::text) AS activity_code,
                   a.id                                AS raw_id,
                   a.activity_name                     AS activity_name,
                   a.start_time                        AS start_time,
                   a.end_time                          AS end_time,
                   a.min_group_size                    AS min_group_size,
                   a.max_group_size                    AS max_group_size,
                   a.status                            AS status,
                   a.create_time                       AS create_time,
                   a.product_id                        AS product_id,
                   a.group_price                       AS group_price,
                   COALESCE(g.group_count, 0)          AS group_count,
                   COALESCE(g.success_group_count, 0)  AS success_group_count
            FROM erp_group_buy_activity a
            LEFT JOIN (
                SELECT activity_id,
                       COUNT(DISTINCT group_id) AS group_count,
                       COUNT(DISTINCT CASE WHEN group_status = 'SUCCESS' THEN group_id END) AS success_group_count
                FROM erp_group_buy_participant
                WHERE deleted = 0
                GROUP BY activity_id
            ) g ON g.activity_id = a.id
            WHERE a.deleted = 0
              AND a.tenant_id = #{tenantId}
              <if test="name != null and name != ''"> AND a.activity_name LIKE CONCAT('%', #{name}, '%') </if>
              <if test="status != null and status != ''"> AND a.status = #{status} </if>
            ORDER BY a.create_time DESC NULLS LAST, a.id DESC
            </script>
            """)
    IPage<GroupBuyActivityRowVO> selectActivityPage(Page<GroupBuyActivityRowVO> page,
                                                    @Param("tenantId") Long tenantId,
                                                    @Param("name") String name,
                                                    @Param("status") String status);

    /** 「拼团订单」Tab：11 列（订单号/单据时间/商品金额/订单金额取自销售单据） */
    @Select("""
            <script>
            SELECT p.id                                  AS id,
                   p.group_id                            AS group_id,
                   COALESCE(p.user_name, bp.party_name)  AS customer_name,
                   prod.product_name                     AS product_name,
                   so.order_no                           AS order_no,
                   p.join_time                           AS submit_time,
                   so.order_date                         AS bill_date,
                   so.product_amount                     AS product_amount,
                   so.total_amount                       AS order_amount,
                   a.activity_name                       AS activity_name,
                   COALESCE(NULLIF(a.activity_code, ''), a.id::text) AS activity_code,
                   p.group_status                        AS group_status,
                   p.activity_id                         AS activity_id,
                   p.order_id                            AS order_id
            FROM erp_group_buy_participant p
            LEFT JOIN erp_group_buy_activity a ON a.id = p.activity_id
            LEFT JOIN erp_product prod         ON prod.id = a.product_id
            LEFT JOIN biz_party bp             ON bp.id = p.customer_id
            LEFT JOIN erp_sale_order so        ON so.id = p.order_id
            WHERE p.deleted = 0
              AND a.tenant_id = #{tenantId}
              <if test="groupId != null and groupId != ''"> AND p.group_id LIKE CONCAT('%', #{groupId}, '%') </if>
              <if test="customer != null and customer != ''"> AND COALESCE(p.user_name, bp.party_name) LIKE CONCAT('%', #{customer}, '%') </if>
              <if test="orderNo != null and orderNo != ''"> AND so.order_no LIKE CONCAT('%', #{orderNo}, '%') </if>
              <if test="groupStatus != null and groupStatus != ''"> AND p.group_status = #{groupStatus} </if>
            ORDER BY p.join_time DESC NULLS LAST, p.id DESC
            </script>
            """)
    IPage<GroupBuyOrderRowVO> selectOrderPage(Page<GroupBuyOrderRowVO> page,
                                              @Param("tenantId") Long tenantId,
                                              @Param("groupId") String groupId,
                                              @Param("customer") String customer,
                                              @Param("orderNo") String orderNo,
                                              @Param("groupStatus") String groupStatus);
}
