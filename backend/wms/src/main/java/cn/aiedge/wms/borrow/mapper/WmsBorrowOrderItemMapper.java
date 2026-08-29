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
                oi.product_id   AS "productId",
                oi.product_code AS "productCode",
                oi.product_name AS "productName",
                oi.product_spec AS "productSpec",
                oi.unit         AS unit,
                o.partner_id    AS "partnerId",
                o.partner_name  AS "partnerName",
                COALESCE(SUM(oi.quantity), 0) AS "borrowQty",
                COALESCE(SUM(oi.amount), 0)   AS "borrowAmount",
                COALESCE(SUM(oi.quantity - oi.returned_quantity), 0) AS "borrowStock",
                COALESCE(SUM((SELECT SUM(ri.quantity)
                              FROM wms_borrow_return_item ri
                              WHERE ri.order_item_id = oi.id AND ri.deleted = 0)), 0) AS "returnedQty",
                COALESCE(SUM((SELECT SUM(ri.quantity * oi.price)
                              FROM wms_borrow_return_item ri
                              WHERE ri.order_item_id = oi.id AND ri.deleted = 0)), 0) AS "returnedAmount"
            FROM wms_borrow_order o
            JOIN wms_borrow_order_item oi ON oi.order_id = o.id AND oi.deleted = 0
            WHERE o.deleted = 0
              AND o.direction = #{direction}
              AND o.status IN (2, 3, 4)
              <if test="partnerName != null and partnerName != ''">
                  AND o.partner_name LIKE CONCAT('%', #{partnerName}, '%')
              </if>
              <if test="productName != null and productName != ''">
                  AND oi.product_name LIKE CONCAT('%', #{productName}, '%')
              </if>
              <if test="dateStart != null and dateStart != ''">
                  AND o.borrow_date &gt;= CAST(#{dateStart} AS DATE)
              </if>
              <if test="dateEnd != null and dateEnd != ''">
                  AND o.borrow_date &lt;= CAST(#{dateEnd} AS DATE)
              </if>
            GROUP BY oi.product_id, oi.product_code, oi.product_name, oi.product_spec, oi.unit,
                     o.partner_id, o.partner_name
            ORDER BY "borrowQty" DESC
            </script>
            """)
    List<Map<String, Object>> aggregateByProduct(@Param("direction") Integer direction,
                                                 @Param("partnerName") String partnerName,
                                                 @Param("productName") String productName,
                                                 @Param("dateStart") String dateStart,
                                                 @Param("dateEnd") String dateEnd);
}
