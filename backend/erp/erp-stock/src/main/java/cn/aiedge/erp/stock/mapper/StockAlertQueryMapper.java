package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.dto.StockAlertQueryVO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 预警查询Mapper
 * <p>
 * 以 erp_stock_alert_config（阈值配置）为主体（每「商品×仓库」配置一行），联商品主数据
 * erp_product 取商品属性，联 erp_stock 汇总当前账面库存，按配置的上下限阈值判断触发预警：
 * <ul>
 *   <li>下限预警（库存不足）：enable_low_stock_alert=1 且 账面库存 &lt; min_stock</li>
 *   <li>上限预警（库存积压）：enable_over_stock_alert=1 且 账面库存 &gt; max_stock</li>
 * </ul>
 * 可空参数一律以 CAST(#{x} AS 类型) 引用，避免 pg 无法推断裸参数类型。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface StockAlertQueryMapper {

    @InterceptorIgnore(tenantLine = "true")
    @Select("WITH RECURSIVE category_scope AS ("
        + " SELECT id FROM erp_product_category"
        + " WHERE CAST(#{categoryId} AS BIGINT) IS NOT NULL AND id = CAST(#{categoryId} AS BIGINT)"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " UNION ALL"
        + " SELECT c.id FROM erp_product_category c JOIN category_scope cs ON c.parent_id = cs.id"
        + " WHERE CAST(#{tenantId} AS BIGINT) IS NULL OR c.tenant_id = CAST(#{tenantId} AS BIGINT)"
        + "), small_unit_scope AS ("
        + " SELECT DISTINCT ON (product_id) product_id, unit_name"
        + " FROM erp_product_unit"
        + " WHERE unit_type = 'SMALL' AND deleted = 0"
        + " ORDER BY product_id, sort_order"
        + "), stock_agg AS ("
        + " SELECT product_id, warehouse_id,"
        + "        SUM(COALESCE(quantity, 0)) AS qty"
        + " FROM erp_stock"
        + " WHERE deleted = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " GROUP BY product_id, warehouse_id"
        + ") "
        + "SELECT"
        + " c.id,"
        + " c.product_id AS product_id,"
        + " COALESCE(NULLIF(p.product_code_alias, ''), p.product_code) AS product_code,"
        + " p.product_name AS product_name,"
        + " NULL AS taste,"
        + " p.model,"
        + " p.unit,"
        + " su.unit_name AS small_unit,"
        + " p.barcode,"
        + " p.spec,"
        + " p.origin,"
        + " p.brand,"
        + " c.warehouse_id AS warehouse_id,"
        + " c.warehouse_name AS warehouse_name,"
        + " c.max_stock AS max_stock,"
        + " c.min_stock AS min_stock,"
        + " c.safety_stock AS safety_stock,"
        + " COALESCE(s.qty, 0) AS book_qty,"
        + " CASE"
        + "   WHEN COALESCE(s.qty, 0) < c.min_stock THEN c.min_stock - COALESCE(s.qty, 0)"
        + "   ELSE COALESCE(s.qty, 0) - c.max_stock"
        + " END AS diff_qty,"
        + " CASE"
        + "   WHEN COALESCE(s.qty, 0) < c.min_stock THEN 'LOW_STOCK'"
        + "   ELSE 'OVER_STOCK'"
        + " END AS alert_type,"
        + " c.remark"
        + " FROM erp_stock_alert_config c"
        + " LEFT JOIN erp_product p ON p.id = c.product_id AND p.deleted = 0"
        + " LEFT JOIN stock_agg s ON s.product_id = c.product_id AND s.warehouse_id = c.warehouse_id"
        + " LEFT JOIN small_unit_scope su ON su.product_id = c.product_id"
        + " WHERE c.deleted = 0 AND c.active = 1"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR c.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + "   AND (CAST(#{warehouseId} AS BIGINT) IS NULL OR c.warehouse_id = CAST(#{warehouseId} AS BIGINT))"
        + "   AND (CAST(#{productKeyword} AS VARCHAR) IS NULL OR CAST(#{productKeyword} AS VARCHAR) = ''"
        + "     OR p.product_name ILIKE '%' || CAST(#{productKeyword} AS VARCHAR) || '%'"
        + "     OR p.product_code ILIKE '%' || CAST(#{productKeyword} AS VARCHAR) || '%'"
        + "     OR p.product_code_alias ILIKE '%' || CAST(#{productKeyword} AS VARCHAR) || '%'"
        + "     OR p.barcode ILIKE '%' || CAST(#{productKeyword} AS VARCHAR) || '%')"
        + "   AND (CAST(#{brand} AS VARCHAR) IS NULL OR CAST(#{brand} AS VARCHAR) = ''"
        + "     OR COALESCE(p.brand, '') ILIKE '%' || CAST(#{brand} AS VARCHAR) || '%')"
        + "   AND (CAST(#{categoryId} AS BIGINT) IS NULL OR p.category_id IN (SELECT id FROM category_scope))"
        + "   AND ("
        + "     (CAST(#{alertType} AS VARCHAR) = 'LOW_STOCK'"
        + "       AND c.enable_low_stock_alert = 1 AND c.min_stock IS NOT NULL AND COALESCE(s.qty, 0) < c.min_stock)"
        + "     OR (CAST(#{alertType} AS VARCHAR) = 'OVER_STOCK'"
        + "       AND c.enable_over_stock_alert = 1 AND c.max_stock IS NOT NULL AND COALESCE(s.qty, 0) > c.max_stock)"
        + "     OR ((CAST(#{alertType} AS VARCHAR) IS NULL OR CAST(#{alertType} AS VARCHAR) = '' OR CAST(#{alertType} AS VARCHAR) = 'ALL')"
        + "       AND ("
        + "         (c.enable_low_stock_alert = 1 AND c.min_stock IS NOT NULL AND COALESCE(s.qty, 0) < c.min_stock)"
        + "         OR (c.enable_over_stock_alert = 1 AND c.max_stock IS NOT NULL AND COALESCE(s.qty, 0) > c.max_stock)"
        + "       ))"
        + "   )"
        + " ORDER BY c.warehouse_id, p.product_name")
    IPage<StockAlertQueryVO> selectAlertPage(Page<StockAlertQueryVO> page,
                                             @Param("tenantId") Long tenantId,
                                             @Param("warehouseId") Long warehouseId,
                                             @Param("productKeyword") String productKeyword,
                                             @Param("brand") String brand,
                                             @Param("categoryId") Long categoryId,
                                             @Param("alertType") String alertType);
}
