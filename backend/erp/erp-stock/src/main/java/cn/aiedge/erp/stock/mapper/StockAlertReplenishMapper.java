package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.dto.StockAlertReplenishVO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 库存预警补货查询Mapper
 * <p>
 * 以 erp_stock 为主体（每商品×仓库一行）：
 * <ul>
 *   <li>待发货：erp_sale_outbound_item.pending_quantity 汇总（销售出库未发货）</li>
 *   <li>待收货：erp_purchase_order_item (quantity - received_quantity) 汇总（在途采购，status IN (2,3,5) 且未关闭未取消）</li>
 *   <li>最近采购：ROW_NUMBER 取该商品+仓库最近一条采购订单明细（日期/供货商/单价）</li>
 * </ul>
 * 可空参数一律以 CAST(#{x} AS 类型) 引用，避免 pg 无法推断裸参数类型。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface StockAlertReplenishMapper {

    @InterceptorIgnore(tenantLine = "true")
    @Select("WITH RECURSIVE pending AS ("
        + " SELECT i.product_id, h.warehouse_id, SUM(i.pending_quantity) AS pending_qty"
        + " FROM erp_sale_outbound_item i"
        + " JOIN erp_sale_outbound h ON h.id = i.outbound_id AND h.deleted = 0"
        + " WHERE i.deleted = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR i.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " GROUP BY i.product_id, h.warehouse_id"
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
        + " SELECT product_id, warehouse_id, supplier_name, order_date, price"
        + " FROM ("
        + "   SELECT i.product_id, COALESCE(i.warehouse_id, h.warehouse_id) AS warehouse_id,"
        + "          h.supplier_name, CAST(h.order_date AS DATE) AS order_date,"
        + "          COALESCE(i.unit_price, i.cost_price) AS price,"
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
        + " s.product_id::varchar || '-' || s.warehouse_id::varchar AS id,"
        + " s.warehouse_id,"
        + " w.warehouse_code,"
        + " COALESCE(s.warehouse_name, w.warehouse_name) AS warehouse_name,"
        + " s.product_id,"
        + " COALESCE(s.product_name, p.product_name) AS product_name,"
        + " COALESCE(NULLIF(p.product_code_alias, ''), COALESCE(s.product_code, p.product_code)) AS product_code,"
        + " p.brand,"
        + " p.weight,"
        + " p.volume,"
        + " NULL::varchar AS taste,"
        + " p.model,"
        + " p.barcode,"
        + " p.spec AS specification,"
        + " p.origin,"
        + " COALESCE(s.unit, p.unit) AS unit,"
        + " CASE"
        + "   WHEN s.quantity <= 0 THEN '缺货'"
        + "   WHEN s.min_stock IS NOT NULL AND s.quantity < s.min_stock THEN '下限预警'"
        + "   WHEN s.max_stock IS NOT NULL AND s.quantity > s.max_stock THEN '超储'"
        + "   ELSE '正常'"
        + " END AS alert_type,"
        + " (COALESCE(s.max_stock, 0) + COALESCE(pp.pending_qty, 0)"
        + "   - COALESCE(s.quantity, 0) - COALESCE(it.transit_qty, 0)) AS shortage_qty,"
        + " s.max_stock,"
        + " s.min_stock,"
        + " COALESCE(s.remark, p.remark) AS remark,"
        + " COALESCE(pp.pending_qty, 0) AS pending_qty,"
        + " s.quantity AS book_qty,"
        + " COALESCE(it.transit_qty, 0) AS in_transit_qty,"
        + " lp.order_date AS last_purchase_date,"
        + " lp.supplier_name AS last_supplier_name,"
        + " lp.price AS last_purchase_price"
        + " FROM erp_stock s"
        + " LEFT JOIN erp_product p ON p.id = s.product_id AND p.deleted = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR p.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " LEFT JOIN erp_warehouse w ON w.id = s.warehouse_id AND w.deleted = 0"
        + " LEFT JOIN pending pp ON pp.product_id = s.product_id AND pp.warehouse_id = s.warehouse_id"
        + " LEFT JOIN in_transit it ON it.product_id = s.product_id AND it.warehouse_id = s.warehouse_id"
        + " LEFT JOIN latest_purchase lp ON lp.product_id = s.product_id AND lp.warehouse_id = s.warehouse_id"
        + " WHERE s.deleted = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR s.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + "   AND (CAST(#{warehouseId} AS BIGINT) IS NULL OR s.warehouse_id = CAST(#{warehouseId} AS BIGINT))"
        + "   AND (CAST(#{keyword} AS VARCHAR) IS NULL OR CAST(#{keyword} AS VARCHAR) = ''"
        + "     OR s.product_name ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%'"
        + "     OR p.product_name ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%'"
        + "     OR s.product_code ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%'"
        + "     OR p.product_code ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%'"
        + "     OR p.product_code_alias ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%')"
        + "   AND (CAST(#{brand} AS VARCHAR) IS NULL OR CAST(#{brand} AS VARCHAR) = '' OR p.brand = CAST(#{brand} AS VARCHAR))"
        + "   AND (CAST(#{supplierName} AS VARCHAR) IS NULL OR CAST(#{supplierName} AS VARCHAR) = ''"
        + "     OR s.supplier_name ILIKE '%' || CAST(#{supplierName} AS VARCHAR) || '%'"
        + "     OR lp.supplier_name ILIKE '%' || CAST(#{supplierName} AS VARCHAR) || '%')"
        + "   AND (CAST(#{remark} AS VARCHAR) IS NULL OR CAST(#{remark} AS VARCHAR) = ''"
        + "     OR s.remark ILIKE '%' || CAST(#{remark} AS VARCHAR) || '%'"
        + "     OR p.remark ILIKE '%' || CAST(#{remark} AS VARCHAR) || '%')"
        + "   AND (CAST(#{onlyLowStock} AS BOOLEAN) IS NOT TRUE"
        + "     OR (s.min_stock IS NOT NULL AND s.quantity < s.min_stock))"
        + "   AND (CAST(#{categoryId} AS BIGINT) IS NULL"
        + "     OR p.category_id IN (SELECT id FROM category_scope))"
        + " ORDER BY s.warehouse_id, s.product_id")
    IPage<StockAlertReplenishVO> selectAlertReplenishPage(Page<StockAlertReplenishVO> page,
                                                          @Param("tenantId") Long tenantId,
                                                          @Param("warehouseId") Long warehouseId,
                                                          @Param("keyword") String keyword,
                                                          @Param("brand") String brand,
                                                          @Param("supplierName") String supplierName,
                                                          @Param("remark") String remark,
                                                          @Param("onlyLowStock") Boolean onlyLowStock,
                                                          @Param("categoryId") Long categoryId);
}
