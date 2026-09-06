package cn.aiedge.wms.borrow.mapper;

import cn.aiedge.wms.entity.WmsBorrowOrderItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface WmsBorrowOrderItemMapper extends BaseMapper<WmsBorrowOrderItem> {

    /**
     * 借进借出商品台账聚合查询（按 商品×往来单位 分组）
     * 方向 1-借进 2-借出；仅统计已生效单据（已审批/部分归还/已归还）
     */
    @Select("""
            <script>
            SELECT
                oi.product_id              AS "product_id",
                COALESCE(NULLIF(oi.product_code,''), p.product_code_alias, p.product_code) AS "product_code",
                oi.product_name            AS "product_name",
                COALESCE(p.spec, oi.product_spec) AS "product_spec",
                oi.unit                    AS unit,
                COALESCE(p.model, oi.model) AS model,
                COALESCE(p.origin, oi.origin) AS origin,
                p.brand                    AS brand,
                COALESCE(p.barcode, oi.barcode) AS barcode,
                p.image_url                AS "image_url",
                oi.small_unit              AS "small_unit",
                oi.conversion_relation     AS "conversion_relation",
                COALESCE(p.remark, oi.remark) AS "product_remark",
                o.partner_id               AS "partner_id",
                COALESCE(o.partner_code, c.party_code) AS "partner_code",
                o.partner_name             AS "partner_name",
                c.party_level              AS "customer_level",
                NULL                       AS contact,
                NULL                       AS address,
                NULL                       AS "default_handler",
                NULL                       AS "one_bill",
                c.remark                   AS "customer_remark",
                -- 台账统计（本期发生 = 单据日期落在 [dateStart, dateEnd]；库存结余 = 截至 dateEnd 的累计未还量）
                COALESCE(SUM(CASE WHEN #{dateStart} IS NULL OR #{dateEnd} IS NULL
                                    OR o.borrow_date BETWEEN CAST(#{dateStart} AS DATE) AND CAST(#{dateEnd} AS DATE)
                                  THEN oi.quantity ELSE 0 END), 0) AS "borrow_query_qty",
                COALESCE(SUM(CASE WHEN #{dateStart} IS NULL OR #{dateEnd} IS NULL
                                    OR o.borrow_date BETWEEN CAST(#{dateStart} AS DATE) AND CAST(#{dateEnd} AS DATE)
                                  THEN COALESCE(oi.amount,0) ELSE 0 END), 0) AS "borrow_query_amount",
                COALESCE(SUM(CASE WHEN #{dateEnd} IS NULL OR o.borrow_date &lt;= CAST(#{dateEnd} AS DATE)
                                  THEN (oi.quantity - COALESCE(oi.returned_quantity,0)) ELSE 0 END), 0) AS "borrow_stock_qty",
                COALESCE(SUM(CASE WHEN #{dateEnd} IS NULL OR o.borrow_date &lt;= CAST(#{dateEnd} AS DATE)
                                  THEN (oi.quantity - COALESCE(oi.returned_quantity,0)) * COALESCE(oi.price,0) ELSE 0 END), 0) AS "borrow_stock_amount",
                COALESCE(SUM(CASE WHEN oi.quantity &gt; 0
                                  AND ( #{dateEnd} IS NULL OR o.borrow_date &lt;= CAST(#{dateEnd} AS DATE) )
                                  THEN COALESCE(oi.conversion_result,0) * (oi.quantity - COALESCE(oi.returned_quantity,0)) / oi.quantity ELSE 0 END), 0) AS "borrow_stock_conversion",
                0 AS "borrow_stock_float",
                COALESCE(SUM(CASE WHEN #{dateStart} IS NULL OR #{dateEnd} IS NULL
                                    OR o.borrow_date BETWEEN CAST(#{dateStart} AS DATE) AND CAST(#{dateEnd} AS DATE)
                                  THEN COALESCE(oi.returned_quantity,0) ELSE 0 END), 0) AS "return_qty",
                COALESCE(SUM(CASE WHEN #{dateStart} IS NULL OR #{dateEnd} IS NULL
                                    OR o.borrow_date BETWEEN CAST(#{dateStart} AS DATE) AND CAST(#{dateEnd} AS DATE)
                                  THEN COALESCE(oi.returned_quantity,0) * COALESCE(oi.price,0) ELSE 0 END), 0) AS "return_amount",
                COALESCE(SUM(CASE WHEN #{dateStart} IS NULL OR #{dateEnd} IS NULL
                                    OR o.borrow_date BETWEEN CAST(#{dateStart} AS DATE) AND CAST(#{dateEnd} AS DATE)
                                  THEN COALESCE(oi.processed_purchase_quantity,0) ELSE 0 END), 0) AS "convert_qty",
                COALESCE(SUM(CASE WHEN #{dateStart} IS NULL OR #{dateEnd} IS NULL
                                    OR o.borrow_date BETWEEN CAST(#{dateStart} AS DATE) AND CAST(#{dateEnd} AS DATE)
                                  THEN COALESCE(oi.processed_purchase_quantity,0) * COALESCE(oi.price,0) ELSE 0 END), 0) AS "convert_amount"
            FROM wms_borrow_order o
            JOIN wms_borrow_order_item oi ON oi.order_id = o.id AND oi.deleted = 0
            LEFT JOIN erp_product p ON p.id = oi.product_id AND p.deleted = 0
            LEFT JOIN biz_party c ON c.id = o.partner_id AND c.deleted = 0
            WHERE o.deleted = 0
              AND o.direction = #{direction}
              AND o.status IN (2, 3, 4)
              <if test="partnerName != null and partnerName != ''">
                  AND o.partner_name LIKE CONCAT('%', #{partnerName}, '%')
              </if>
              <if test="productName != null and productName != ''">
                  AND oi.product_name LIKE CONCAT('%', #{productName}, '%')
              </if>
              <if test="dateEnd != null and dateEnd != ''">
                  AND o.borrow_date &lt;= CAST(#{dateEnd} AS DATE)
              </if>
              <if test="categoryId != null">
                  AND p.category_id = #{categoryId}
              </if>
              <if test="handlerName != null and handlerName != ''">
                  AND o.handler_name LIKE CONCAT('%', #{handlerName}, '%')
              </if>
              <if test="deptName != null and deptName != ''">
                  AND o.dept_name LIKE CONCAT('%', #{deptName}, '%')
              </if>
            GROUP BY oi.product_id, oi.product_code, oi.product_name, oi.unit, oi.product_spec, oi.model, oi.origin,
                     oi.barcode, oi.small_unit, oi.conversion_relation, oi.remark,
                     p.product_code, p.product_code_alias, p.spec, p.model, p.origin, p.brand, p.barcode, p.image_url, p.remark,
                     o.partner_id, o.partner_code, o.partner_name,
                     c.party_code, c.party_level, c.remark
            ORDER BY "borrow_query_qty" DESC
            </script>
            """)
    List<Map<String, Object>> aggregateByProduct(@Param("direction") Integer direction,
                                                 @Param("partnerName") String partnerName,
                                                 @Param("productName") String productName,
                                                 @Param("dateStart") String dateStart,
                                                 @Param("dateEnd") String dateEnd,
                                                 @Param("categoryId") Long categoryId,
                                                 @Param("handlerName") String handlerName,
                                                 @Param("deptName") String deptName);
}
