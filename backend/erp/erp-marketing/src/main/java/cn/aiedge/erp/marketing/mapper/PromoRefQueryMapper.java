package cn.aiedge.erp.marketing.mapper;

import cn.aiedge.erp.marketing.dto.PromoCustomerRow;
import cn.aiedge.erp.marketing.dto.PromoProductRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 促销活动关联对象查询（查看商品 / 查看客户）——手写 SQL，显式带 tenant_id */
@Mapper
public interface PromoRefQueryMapper {

    @Select("""
            <script>
            SELECT p.id            AS id,
                   p.product_code  AS product_code,
                   p.product_name  AS product_name,
                   p.spec          AS spec,
                   p.unit          AS unit,
                   pu.retail_price    AS retail_price,
                   pu.wholesale_price AS wholesale_price
            FROM erp_product p
            LEFT JOIN erp_product_unit pu
                   ON pu.product_id = p.id AND pu.deleted = 0 AND pu.is_base_unit = 1
            WHERE p.deleted = 0
              AND p.tenant_id = #{tenantId}
              AND p.id IN
              <foreach collection="ids" item="i" open="(" separator="," close=")">#{i}</foreach>
            ORDER BY p.product_code
            </script>
            """)
    List<PromoProductRow> selectPromoProducts(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);

    @Select("""
            <script>
            SELECT p.id            AS id,
                   p.party_code    AS party_code,
                   p.party_name    AS party_name,
                   p.member_level  AS member_level,
                   COALESCE(pc.mobile, pc.phone, p.phone) AS phone
            FROM biz_party p
            LEFT JOIN biz_party_contact pc
                   ON pc.party_id = p.id AND pc.deleted = 0 AND pc.is_primary = 1
            WHERE p.deleted = 0
              AND p.tenant_id = #{tenantId}
              AND p.id IN
              <foreach collection="ids" item="i" open="(" separator="," close=")">#{i}</foreach>
            ORDER BY p.party_code
            </script>
            """)
    List<PromoCustomerRow> selectPromoCustomers(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);
}
