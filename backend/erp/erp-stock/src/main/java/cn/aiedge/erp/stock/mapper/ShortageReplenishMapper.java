package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.dto.ShortageReplenishVO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 缺货补货查询Mapper
 * <p>
 * 以「有效销售订单明细」按商品×仓库聚合为主体：
 * <ul>
 *   <li>订单数量/价税合计/已发货/待发货：erp_sale_order_item 汇总（仅统计有效销售订单，排除草稿/待审批/已取消）</li>
 *   <li>待收货：erp_purchase_order_item (quantity - received_quantity) 汇总（在途采购，status IN (2,3,5) 且未关闭未取消）</li>
 *   <li>账面库存：erp_stock.quantity</li>
 *   <li>缺货数量按前端选区计算，非负；仅显示缺货商品时过滤 &gt; 0</li>
 * </ul>
 * 可空参数一律以 CAST(#{x} AS 类型) 引用，避免 pg 无法推断裸参数类型。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface ShortageReplenishMapper {

    @InterceptorIgnore(tenantLine = "true")
    @Select("WITH RECURSIVE sale_agg AS ("
        + " SELECT i.product_id,"
        + "        COALESCE(i.warehouse_id, o.warehouse_id) AS warehouse_id,"
        + "        SUM(COALESCE(i.quantity, 0)) AS order_qty,"
        + "        SUM(COALESCE(i.amount_with_tax, 0)) AS amount_with_tax,"
        + "        SUM(COALESCE(i.shipped_quantity, 0)) AS shipped_qty,"
        + "        SUM(COALESCE(i.unshipped_quantity, 0)) AS unshipped_qty,"
        + "        MIN(i.product_name) AS product_name,"
        + "        MIN(i.item_code) AS item_code,"
        + "        MIN(i.product_code) AS product_code,"
        + "        MIN(i.image) AS image,"
        + "        MIN(i.specification) AS specification,"
        + "        MIN(i.model) AS model,"
        + "        MIN(i.origin) AS origin,"
        + "        MIN(i.brand) AS brand,"
        + "        MIN(i.unit) AS unit,"
        + "        MIN(i.remark) AS remark"
        + " FROM erp_sale_order_item i"
        + " JOIN erp_sale_order o ON o.id = i.order_id AND o.deleted = 0"
        + " WHERE (CAST(#{tenantId} AS BIGINT) IS NULL OR o.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR i.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + "   AND ("
        + "     (CAST(#{orderStatus} AS INTEGER) IS NOT NULL AND o.status = CAST(#{orderStatus} AS INTEGER))"
        + "     OR (CAST(#{orderStatus} AS INTEGER) IS NULL AND o.status IN (2,3,4,5))"
        + "   )"
        + "   AND (CAST(#{startDate} AS DATE) IS NULL OR o.order_date::date >= CAST(#{startDate} AS DATE))"
        + "   AND (CAST(#{endDate} AS DATE) IS NULL OR o.order_date::date <= CAST(#{endDate} AS DATE))"
        + "   AND (CAST(#{customerId} AS BIGINT) IS NULL OR o.customer_id = CAST(#{customerId} AS BIGINT))"
        + "   AND (CAST(#{salesmanId} AS BIGINT) IS NULL OR o.salesman_id = CAST(#{salesmanId} AS BIGINT))"
        + "   AND (CAST(#{warehouseId} AS BIGINT) IS NULL"
        + "     OR COALESCE(i.warehouse_id, o.warehouse_id) = CAST(#{warehouseId} AS BIGINT))"
        + "   AND (CAST(#{orderSource} AS INTEGER) IS NULL OR o.order_source = CAST(#{orderSource} AS INTEGER))"
        + "   AND (CAST(#{productKeyword} AS VARCHAR) IS NULL OR CAST(#{productKeyword} AS VARCHAR) = ''"
        + "     OR i.product_name ILIKE '%' || CAST(#{productKeyword} AS VARCHAR) || '%'"
        + "     OR i.product_code ILIKE '%' || CAST(#{productKeyword} AS VARCHAR) || '%'"
        + "     OR i.item_code ILIKE '%' || CAST(#{productKeyword} AS VARCHAR) || '%')"
        + " GROUP BY i.product_id, COALESCE(i.warehouse_id, o.warehouse_id)"
        + "), in_transit AS ("
        + " SELECT i.product_id, COALESCE(i.warehouse_id, h.warehouse_id) AS warehouse_id,"
        + "        SUM(i.quantity - COALESCE(i.received_quantity, 0)) AS transit_qty"
        + " FROM erp_purchase_order_item i"
        + " JOIN erp_purchase_order h ON h.id = i.order_id AND h.deleted = 0"
        + " WHERE h.status IN (2,3,5)"
        + "   AND COALESCE(h.closed_flag, 0) = 0"
        + "   AND COALESCE(h.cancellation_flag, 0) = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR h.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR i.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " GROUP BY i.product_id, COALESCE(i.warehouse_id, h.warehouse_id)"
        + "), latest_purchase AS ("
        + " SELECT product_id, warehouse_id, supplier_name"
        + " FROM ("
        + "   SELECT i.product_id, COALESCE(i.warehouse_id, h.warehouse_id) AS warehouse_id,"
        + "          h.supplier_name,"
        + "          ROW_NUMBER() OVER (PARTITION BY i.product_id, COALESCE(i.warehouse_id, h.warehouse_id)"
        + "            ORDER BY h.order_date DESC, h.id DESC) AS rn"
        + "   FROM erp_purchase_order_item i"
        + "   JOIN erp_purchase_order h ON h.id = i.order_id AND h.deleted = 0"
        + "   WHERE (CAST(#{tenantId} AS BIGINT) IS NULL OR h.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + "     AND (CAST(#{tenantId} AS BIGINT) IS NULL OR i.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " ) t WHERE t.rn = 1"
        + "), category_scope AS ("
        + " SELECT id FROM erp_product_category"
        + " WHERE CAST(#{categoryId} AS BIGINT) IS NOT NULL AND id = CAST(#{categoryId} AS BIGINT)"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " UNION ALL"
        + " SELECT c.id FROM erp_product_category c JOIN category_scope cs ON c.parent_id = cs.id"
        + " WHERE (CAST(#{tenantId} AS BIGINT) IS NULL OR c.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + ") "
        + "SELECT"
        + " sa.product_id::varchar || '-' || sa.warehouse_id::varchar AS id,"
        + " sa.warehouse_id,"
        + " COALESCE(s.warehouse_name, w.warehouse_name, '') AS warehouse_name,"
        + " sa.product_id,"
        + " sa.image,"
        + " sa.product_name,"
        + " COALESCE(sa.item_code, sa.product_code) AS product_code,"
        + " sa.specification,"
        + " sa.model,"
        + " sa.origin,"
        + " sa.brand,"
        + " sa.unit,"
        + " sa.order_qty,"
        + " sa.amount_with_tax,"
        + " sa.shipped_qty,"
        + " sa.unshipped_qty,"
        + " COALESCE(it.transit_qty, 0) AS in_transit_qty,"
        + " COALESCE(s.quantity, 0) AS book_qty,"
        + " CASE"
        + "   WHEN CAST(#{shortageMode} AS INTEGER) = 2"
        + "     THEN GREATEST(0, COALESCE(sa.unshipped_qty,0) - COALESCE(it.transit_qty,0) - COALESCE(s.quantity,0))"
        + "   ELSE GREATEST(0, COALESCE(sa.unshipped_qty,0) - COALESCE(s.quantity,0))"
        + " END AS shortage_qty,"
        + " sa.remark,"
        + " COALESCE(s.supplier_name, lp.supplier_name) AS supplier_name"
        + " FROM sale_agg sa"
        + " LEFT JOIN erp_product p ON p.id = sa.product_id AND p.deleted = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR p.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " LEFT JOIN erp_stock s ON s.product_id = sa.product_id AND s.warehouse_id = sa.warehouse_id AND s.deleted = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR s.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " LEFT JOIN erp_warehouse w ON w.id = sa.warehouse_id AND w.deleted = 0"
        + " LEFT JOIN in_transit it ON it.product_id = sa.product_id AND it.warehouse_id = sa.warehouse_id"
        + " LEFT JOIN latest_purchase lp ON lp.product_id = sa.product_id AND lp.warehouse_id = sa.warehouse_id"
        + " WHERE (CAST(#{supplierName} AS VARCHAR) IS NULL OR CAST(#{supplierName} AS VARCHAR) = ''"
        + "     OR COALESCE(s.supplier_name, lp.supplier_name) ILIKE '%' || CAST(#{supplierName} AS VARCHAR) || '%')"
        + "   AND (CAST(#{categoryId} AS BIGINT) IS NULL OR p.category_id IN (SELECT id FROM category_scope))"
        + "   AND (CAST(#{onlyShortage} AS BOOLEAN) IS NOT TRUE OR ("
        + "     CASE"
        + "       WHEN CAST(#{shortageMode} AS INTEGER) = 2"
        + "         THEN GREATEST(0, COALESCE(sa.unshipped_qty,0) - COALESCE(it.transit_qty,0) - COALESCE(s.quantity,0))"
        + "       ELSE GREATEST(0, COALESCE(sa.unshipped_qty,0) - COALESCE(s.quantity,0))"
        + "     END) > 0)"
        + " ORDER BY sa.warehouse_id, sa.product_id")
    IPage<ShortageReplenishVO> selectShortagePage(Page<ShortageReplenishVO> page,
                                                  @Param("tenantId") Long tenantId,
                                                  @Param("orderStatus") Integer orderStatus,
                                                  @Param("startDate") String startDate,
                                                  @Param("endDate") String endDate,
                                                  @Param("customerId") Long customerId,
                                                  @Param("salesmanId") Long salesmanId,
                                                  @Param("warehouseId") Long warehouseId,
                                                  @Param("orderSource") Integer orderSource,
                                                  @Param("productKeyword") String productKeyword,
                                                  @Param("supplierName") String supplierName,
                                                  @Param("categoryId") Long categoryId,
                                                  @Param("shortageMode") Integer shortageMode,
                                                  @Param("onlyShortage") Boolean onlyShortage);
}
