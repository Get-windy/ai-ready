package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.dto.SmartReplenishVO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 智能补货查询Mapper
 * <p>
 * 以 erp_product 为主体（每商品一行，商品档案为中心），聚合：
 * <ul>
 *   <li>销售数量/金额：erp_sale_order_item 汇总（销售日期区间，仅统计有效销售订单 status IN (2,3,4,5)）</li>
 *   <li>采购数量/金额：erp_purchase_order_item 汇总（销售日期区间，仅统计有效采购 status IN (2,3,5) 且未关闭未取消）</li>
 *   <li>待收货：erp_purchase_order_item (quantity - received_quantity)（在途采购，当前口径）</li>
 *   <li>待发货：erp_sale_outbound_item.pending_quantity（已售未出库，当前口径）</li>
 *   <li>账面库存：erp_stock.quantity；可用库存：erp_stock.available_quantity</li>
 *   <li>换算结果：按该商品最大非基础单位 conversion_rate 换算（无多单位时等于原值）</li>
 *   <li>计划采购数量 = 备货天数×日均销量 + 待发货数量 - 待收货数量 - 账面库存</li>
 * </ul>
 * 可空参数一律以 CAST(#{x} AS 类型) 引用，避免 pg 无法推断裸参数类型。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SmartReplenishMapper {

    @InterceptorIgnore(tenantLine = "true")
    @Select("WITH RECURSIVE sales_agg AS ("
        + " SELECT i.product_id,"
        + "        SUM(COALESCE(i.quantity, 0)) AS sales_qty,"
        + "        SUM(COALESCE(i.amount_with_tax, i.unit_price * i.quantity, 0)) AS sales_amount,"
        + "        MAX(CAST(o.order_date AS DATE)) AS last_sale_date"
        + " FROM erp_sale_order_item i"
        + " JOIN erp_sale_order o ON o.id = i.order_id AND o.deleted = 0"
        + " WHERE (CAST(#{tenantId} AS BIGINT) IS NULL OR o.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR i.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + "   AND (CAST(#{warehouseId} AS BIGINT) IS NULL OR COALESCE(i.warehouse_id, o.warehouse_id) = CAST(#{warehouseId} AS BIGINT))"
        + "   AND (CAST(#{startDate} AS DATE) IS NULL OR CAST(o.order_date AS DATE) >= CAST(#{startDate} AS DATE))"
        + "   AND (CAST(#{endDate} AS DATE) IS NULL OR CAST(o.order_date AS DATE) <= CAST(#{endDate} AS DATE))"
        + " GROUP BY i.product_id"
        + "), purchase_agg AS ("
        + " SELECT i.product_id,"
        + "        SUM(COALESCE(i.quantity, 0)) AS purchase_qty,"
        + "        SUM(COALESCE(i.amount_with_tax, i.unit_price * i.quantity, 0)) AS purchase_amount,"
        + "        MAX(CAST(h.order_date AS DATE)) AS last_purchase_date"
        + " FROM erp_purchase_order_item i"
        + " JOIN erp_purchase_order h ON h.id = i.order_id AND h.deleted = 0"
        + " WHERE h.status IN (2,3,5)"
        + "   AND COALESCE(h.closed_flag, 0) = 0"
        + "   AND COALESCE(h.cancellation_flag, 0) = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR h.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR i.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + "   AND (CAST(#{warehouseId} AS BIGINT) IS NULL OR COALESCE(i.warehouse_id, h.warehouse_id) = CAST(#{warehouseId} AS BIGINT))"
        + "   AND (CAST(#{startDate} AS DATE) IS NULL OR CAST(h.order_date AS DATE) >= CAST(#{startDate} AS DATE))"
        + "   AND (CAST(#{endDate} AS DATE) IS NULL OR CAST(h.order_date AS DATE) <= CAST(#{endDate} AS DATE))"
        + " GROUP BY i.product_id"
        + "), in_transit AS ("
        + " SELECT i.product_id,"
        + "        SUM(i.quantity - COALESCE(i.received_quantity, 0)) AS transit_qty"
        + " FROM erp_purchase_order_item i"
        + " JOIN erp_purchase_order h ON h.id = i.order_id AND h.deleted = 0"
        + " WHERE h.status IN (2,3,5)"
        + "   AND COALESCE(h.closed_flag, 0) = 0"
        + "   AND COALESCE(h.cancellation_flag, 0) = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR h.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR i.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + "   AND (CAST(#{warehouseId} AS BIGINT) IS NULL OR COALESCE(i.warehouse_id, h.warehouse_id) = CAST(#{warehouseId} AS BIGINT))"
        + " GROUP BY i.product_id"
        + "), pending_ship AS ("
        + " SELECT i.product_id, SUM(COALESCE(i.pending_quantity, 0)) AS pending_qty"
        + " FROM erp_sale_outbound_item i"
        + " JOIN erp_sale_outbound h ON h.id = i.outbound_id AND h.deleted = 0"
        + " WHERE i.deleted = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR i.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + "   AND (CAST(#{warehouseId} AS BIGINT) IS NULL OR h.warehouse_id = CAST(#{warehouseId} AS BIGINT))"
        + " GROUP BY i.product_id"
        + "), stock_agg AS ("
        + " SELECT product_id,"
        + "        SUM(COALESCE(quantity, 0)) AS book_qty,"
        + "        SUM(COALESCE(available_quantity, 0)) AS avail_qty"
        + " FROM erp_stock"
        + " WHERE deleted = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR tenant_id = CAST(#{tenantId} AS BIGINT))"
        + "   AND (CAST(#{warehouseId} AS BIGINT) IS NULL OR warehouse_id = CAST(#{warehouseId} AS BIGINT))"
        + " GROUP BY product_id"
        + "), latest_purchase AS ("
        + " SELECT product_id, supplier_name"
        + " FROM ("
        + "   SELECT i.product_id, h.supplier_name,"
        + "          ROW_NUMBER() OVER (PARTITION BY i.product_id"
        + "            ORDER BY h.order_date DESC, h.id DESC) AS rn"
        + "   FROM erp_purchase_order_item i"
        + "   JOIN erp_purchase_order h ON h.id = i.order_id AND h.deleted = 0"
        + "   WHERE h.status IN (2,3,5)"
        + "     AND COALESCE(h.closed_flag, 0) = 0"
        + "     AND COALESCE(h.cancellation_flag, 0) = 0"
        + "     AND (CAST(#{tenantId} AS BIGINT) IS NULL OR h.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + "     AND (CAST(#{tenantId} AS BIGINT) IS NULL OR i.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + "     AND (CAST(#{warehouseId} AS BIGINT) IS NULL OR COALESCE(i.warehouse_id, h.warehouse_id) = CAST(#{warehouseId} AS BIGINT))"
        + " ) t WHERE t.rn = 1"
        + "), product_conv AS ("
        + " SELECT product_id,"
        + "        MAX(conversion_rate) AS max_rate,"
        + "        MAX(CASE WHEN is_base_unit = 1 THEN unit_name END) AS base_unit"
        + " FROM erp_product_unit"
        + " WHERE deleted = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " GROUP BY product_id"
        + "), category_scope AS ("
        + " SELECT id FROM erp_product_category"
        + " WHERE CAST(#{categoryId} AS BIGINT) IS NOT NULL AND id = CAST(#{categoryId} AS BIGINT)"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " UNION ALL"
        + " SELECT c.id FROM erp_product_category c JOIN category_scope cs ON c.parent_id = cs.id"
        + " WHERE (CAST(#{tenantId} AS BIGINT) IS NULL OR c.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + ") "
        + "SELECT"
        + " p.id::varchar AS id,"
        + " p.id AS product_id,"
        + " p.image_url AS image,"
        + " p.product_name,"
        + " COALESCE(NULLIF(p.product_code_alias, ''), p.product_code) AS product_code,"
        + " COALESCE(NULLIF(p.unit, ''), c.base_unit) AS unit,"
        + " p.barcode,"
        + " p.model,"
        + " p.origin,"
        + " p.brand,"
        + " p.remark,"
        + " p.spec AS specification,"
        + " COALESCE(sa.sales_qty, 0) AS sales_qty,"
        + " COALESCE(sa.sales_amount, 0) AS sales_amount,"
        + " COALESCE(pa.purchase_amount, 0) AS purchase_amount,"
        + " CASE WHEN CAST(#{days} AS INTEGER) > 0"
        + "   THEN ROUND(COALESCE(sa.sales_qty, 0) / CAST(#{days} AS NUMERIC), 4)"
        + "   ELSE 0 END AS avg_daily_sales,"
        + " CAST(#{stockDays} AS INTEGER) AS stock_days,"
        + " COALESCE(it.transit_qty, 0) AS in_transit_qty,"
        + " COALESCE(ps.pending_qty, 0) AS pending_ship_qty,"
        + " COALESCE(pa.purchase_qty, 0) AS purchase_qty,"
        + " COALESCE(st.book_qty, 0) AS book_qty,"
        + " ROUND(COALESCE(st.book_qty, 0) / NULLIF(COALESCE(c.max_rate, 1), 0), 4) AS book_qty_converted,"
        + " ROUND(CAST(#{stockDays} AS NUMERIC) * (CASE WHEN CAST(#{days} AS INTEGER) > 0"
        + "   THEN ROUND(COALESCE(sa.sales_qty, 0) / CAST(#{days} AS NUMERIC), 4) ELSE 0 END)"
        + "   + COALESCE(ps.pending_qty, 0) - COALESCE(it.transit_qty, 0) - COALESCE(st.book_qty, 0), 4) AS plan_purchase_qty,"
        + " COALESCE(st.avail_qty, 0) AS available_qty,"
        + " ROUND(COALESCE(st.avail_qty, 0) / NULLIF(COALESCE(c.max_rate, 1), 0), 4) AS available_qty_converted,"
        + " sa.last_sale_date,"
        + " pa.last_purchase_date"
        + " FROM erp_product p"
        + " LEFT JOIN sales_agg sa ON sa.product_id = p.id"
        + " LEFT JOIN purchase_agg pa ON pa.product_id = p.id"
        + " LEFT JOIN in_transit it ON it.product_id = p.id"
        + " LEFT JOIN pending_ship ps ON ps.product_id = p.id"
        + " LEFT JOIN stock_agg st ON st.product_id = p.id"
        + " LEFT JOIN product_conv c ON c.product_id = p.id"
        + " LEFT JOIN latest_purchase lp ON lp.product_id = p.id"
        + " WHERE p.deleted = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR p.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + "   AND (CAST(#{productKeyword} AS VARCHAR) IS NULL OR CAST(#{productKeyword} AS VARCHAR) = ''"
        + "     OR p.product_name ILIKE '%' || CAST(#{productKeyword} AS VARCHAR) || '%'"
        + "     OR p.product_code ILIKE '%' || CAST(#{productKeyword} AS VARCHAR) || '%'"
        + "     OR p.product_code_alias ILIKE '%' || CAST(#{productKeyword} AS VARCHAR) || '%'"
        + "     OR p.barcode ILIKE '%' || CAST(#{productKeyword} AS VARCHAR) || '%')"
        + "   AND (CAST(#{supplierName} AS VARCHAR) IS NULL OR CAST(#{supplierName} AS VARCHAR) = ''"
        + "     OR COALESCE(lp.supplier_name, (SELECT MIN(s2.supplier_name) FROM erp_stock s2"
        + "         WHERE s2.product_id = p.id AND s2.deleted = 0"
        + "           AND (CAST(#{tenantId} AS BIGINT) IS NULL OR s2.tenant_id = CAST(#{tenantId} AS BIGINT))), '')"
        + "       ILIKE '%' || CAST(#{supplierName} AS VARCHAR) || '%')"
        + "   AND (CAST(#{categoryId} AS BIGINT) IS NULL OR p.category_id IN (SELECT id FROM category_scope))"
        + "   AND (CAST(#{minPlanQty} AS NUMERIC) IS NULL"
        + "     OR ROUND(CAST(#{stockDays} AS NUMERIC) * (CASE WHEN CAST(#{days} AS INTEGER) > 0"
        + "       THEN ROUND(COALESCE(sa.sales_qty, 0) / CAST(#{days} AS NUMERIC), 4) ELSE 0 END)"
        + "       + COALESCE(ps.pending_qty, 0) - COALESCE(it.transit_qty, 0) - COALESCE(st.book_qty, 0), 4)"
        + "       >= CAST(#{minPlanQty} AS NUMERIC))"
        + " ORDER BY p.id")
    IPage<SmartReplenishVO> selectSmartReplenishPage(Page<SmartReplenishVO> page,
                                                     @Param("tenantId") Long tenantId,
                                                     @Param("startDate") String startDate,
                                                     @Param("endDate") String endDate,
                                                     @Param("days") Integer days,
                                                     @Param("stockDays") Integer stockDays,
                                                     @Param("warehouseId") Long warehouseId,
                                                     @Param("productKeyword") String productKeyword,
                                                     @Param("supplierName") String supplierName,
                                                     @Param("categoryId") Long categoryId,
                                                     @Param("minPlanQty") BigDecimal minPlanQty);
}
