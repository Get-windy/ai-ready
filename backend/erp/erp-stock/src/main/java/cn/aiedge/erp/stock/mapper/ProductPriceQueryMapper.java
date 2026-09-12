package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.dto.ProductPriceVO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品价格管理（子标签「商品价格批量修改」）查询 Mapper
 * <p>
 * 一行 = 商品 × 单位，价格口径全部取 {@code erp_product_unit}（零售价/批发价/最低售价/最低折扣/
 * 预设进价/参考成本/最近进价 + grade_price_1~8），商品信息取 {@code erp_product}；
 * 账面库存取库存表按商品汇总，最近进货日期取状态为已入库(8)/已完成(9)的采购入库单最大单据日期。
 * 与《商品条码》保持同一套分类递归 / 换算关系口径，避免同一主数据两套算法。
 */
@Mapper
public interface ProductPriceQueryMapper {

    String CTE_PREFIX = "WITH RECURSIVE category_scope AS ("
        + " SELECT id FROM erp_product_category"
        + " WHERE CAST(#{categoryId} AS BIGINT) IS NOT NULL AND id = CAST(#{categoryId} AS BIGINT)"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " UNION ALL"
        + " SELECT c.id FROM erp_product_category c JOIN category_scope cs ON c.parent_id = cs.id"
        + " WHERE CAST(#{tenantId} AS BIGINT) IS NULL OR c.tenant_id = CAST(#{tenantId} AS BIGINT)"
        + "), purchase AS ("
        + " SELECT it.product_id AS product_id, MAX(i.inbound_date) AS last_purchase_date"
        + " FROM erp_purchase_inbound i"
        + " JOIN erp_purchase_inbound_item it ON it.inbound_id = i.id AND it.deleted = 0"
        + " WHERE i.deleted = 0 AND i.status IN (8, 9)"
        + " GROUP BY it.product_id"
        + "), stock AS ("
        + " SELECT s.product_id AS product_id, SUM(s.quantity) AS qty"
        + " FROM erp_stock s"
        + " WHERE s.deleted = 0"
        + " GROUP BY s.product_id"
        + ") ";

    String SELECT_BODY = "SELECT"
        + " u.id AS unit_id,"
        + " p.id AS product_id,"
        + " p.image_url AS image_url,"
        + " p.mall_shelf_status AS shelf_status,"
        + " p.product_code AS product_code,"
        + " p.product_name AS product_name,"
        + " u.unit_name AS unit_name,"
        + " u.is_base_unit AS is_base_unit,"
        + " u.conversion_rate AS conversion_rate,"
        + " p.brand AS brand,"
        + " COALESCE(NULLIF(u.barcode, ''), p.barcode) AS barcode,"
        + " u.recent_purchase_price AS recent_purchase_price,"
        + " u.preset_purchase_price AS preset_purchase_price,"
        + " u.reference_cost AS reference_cost,"
        + " p.cost_price AS cost_avg_price,"
        + " COALESCE(st.qty, 0) AS stock_qty,"
        + " u.wholesale_price AS wholesale_price,"
        + " u.min_discount AS min_discount,"
        + " u.min_sale_price AS min_sale_price,"
        + " u.retail_price AS retail_price,"
        + " u.grade_price_1 AS grade_price1,"
        + " u.grade_price_2 AS grade_price2,"
        + " u.grade_price_3 AS grade_price3,"
        + " u.grade_price_4 AS grade_price4,"
        + " u.grade_price_5 AS grade_price5,"
        + " u.grade_price_6 AS grade_price6,"
        + " u.grade_price_7 AS grade_price7,"
        + " u.grade_price_8 AS grade_price8,"
        + " p.spec AS spec,"
        + " pu.last_purchase_date AS last_purchase_date,"
        + " p.model AS model,"
        + " p.origin AS origin,"
        + " u.update_time AS update_time,"
        + " (SELECT bu.unit_name FROM erp_product_unit bu"
        + "   WHERE bu.product_id = p.id AND bu.is_base_unit = 1 AND bu.deleted = 0"
        + "   ORDER BY bu.sort_order, bu.id LIMIT 1) AS base_unit_name"
        + " FROM erp_product_unit u"
        + " JOIN erp_product p ON p.id = u.product_id AND p.deleted = 0"
        + " LEFT JOIN purchase pu ON pu.product_id = p.id"
        + " LEFT JOIN stock st ON st.product_id = p.id"
        + " WHERE u.deleted = 0";

    /** 日期比较符（对标下拉：&lt; = &gt; ≠ ≤ ≥） */
    String PURCHASE_DATE_OP_BODY =
        " CASE COALESCE(NULLIF(CAST(#{purchaseDateOp} AS VARCHAR), ''), '&gt;=')"
        + "   WHEN '&lt;'  THEN pu.last_purchase_date &lt; CAST(CAST(#{purchaseDate} AS VARCHAR) AS DATE)"
        + "   WHEN '&gt;' THEN pu.last_purchase_date &gt; CAST(CAST(#{purchaseDate} AS VARCHAR) AS DATE)"
        + "   WHEN '&lt;=' THEN pu.last_purchase_date &lt;= CAST(CAST(#{purchaseDate} AS VARCHAR) AS DATE)"
        + "   WHEN '='  THEN pu.last_purchase_date = CAST(CAST(#{purchaseDate} AS VARCHAR) AS DATE)"
        + "   WHEN '!=' THEN pu.last_purchase_date &lt;&gt; CAST(CAST(#{purchaseDate} AS VARCHAR) AS DATE)"
        + "   ELSE pu.last_purchase_date &gt;= CAST(CAST(#{purchaseDate} AS VARCHAR) AS DATE)"
        + " END";

    String STOCK_OP_BODY =
        " CASE COALESCE(NULLIF(CAST(#{stockQtyOp} AS VARCHAR), ''), '&gt;=')"
        + "   WHEN '&lt;'  THEN COALESCE(st.qty, 0) &lt; CAST(CAST(#{stockQty} AS NUMERIC) AS NUMERIC)"
        + "   WHEN '&gt;' THEN COALESCE(st.qty, 0) &gt; CAST(CAST(#{stockQty} AS NUMERIC) AS NUMERIC)"
        + "   WHEN '&lt;=' THEN COALESCE(st.qty, 0) &lt;= CAST(CAST(#{stockQty} AS NUMERIC) AS NUMERIC)"
        + "   WHEN '='  THEN COALESCE(st.qty, 0) = CAST(CAST(#{stockQty} AS NUMERIC) AS NUMERIC)"
        + "   WHEN '!=' THEN COALESCE(st.qty, 0) &lt;&gt; CAST(CAST(#{stockQty} AS NUMERIC) AS NUMERIC)"
        + "   ELSE COALESCE(st.qty, 0) &gt;= CAST(CAST(#{stockQty} AS NUMERIC) AS NUMERIC)"
        + " END";

    String WHERE_BODY = " AND (CAST(#{tenantId} AS BIGINT) IS NULL OR u.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " AND (CAST(#{categoryId} AS BIGINT) IS NULL OR p.category_id IN (SELECT id FROM category_scope))"
        + " AND (CAST(#{keyword} AS VARCHAR) IS NULL OR CAST(#{keyword} AS VARCHAR) = ''"
        + "   OR p.product_name ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%'"
        + "   OR p.product_code ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%'"
        + "   OR p.barcode ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%'"
        + "   OR p.spec ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%'"
        + "   OR p.model ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%')"
        + " AND (CAST(#{brand} AS VARCHAR) IS NULL OR CAST(#{brand} AS VARCHAR) = ''"
        + "   OR p.brand = CAST(#{brand} AS VARCHAR))"
        + " AND (CAST(#{unitType} AS VARCHAR) IS NULL OR CAST(#{unitType} AS VARCHAR) = ''"
        + "   OR u.unit_type = CAST(#{unitType} AS VARCHAR))"
        + " AND (CAST(#{productId} AS BIGINT) IS NULL OR p.id = CAST(#{productId} AS BIGINT))"
        + " AND (CAST(#{shelfStatus} AS INTEGER) IS NULL OR p.mall_shelf_status = CAST(#{shelfStatus} AS INTEGER))"
        + " AND (CAST(#{purchaseDate} AS VARCHAR) IS NULL OR CAST(#{purchaseDate} AS VARCHAR) = ''"
        + "   OR (" + PURCHASE_DATE_OP_BODY + "))"
        + " AND (CAST(#{stockQty} AS VARCHAR) IS NULL OR CAST(#{stockQty} AS VARCHAR) = ''"
        + "   OR (" + STOCK_OP_BODY + "))";

    /** 排序：勾选「显示层次结构」时先按商品分类聚合，否则按货号 */
    String ORDER_BODY = " ORDER BY <if test=\"showHierarchy != null and showHierarchy\">p.category_id,</if>"
        + " p.product_code, p.id, u.sort_order, u.id";

    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>" + CTE_PREFIX + SELECT_BODY + WHERE_BODY + ORDER_BODY + "</script>")
    IPage<ProductPriceVO> selectPricePage(Page<ProductPriceVO> page,
                                          @Param("tenantId") Long tenantId,
                                          @Param("categoryId") Long categoryId,
                                          @Param("keyword") String keyword,
                                          @Param("brand") String brand,
                                          @Param("unitType") String unitType,
                                          @Param("productId") Long productId,
                                          @Param("shelfStatus") Integer shelfStatus,
                                          @Param("purchaseDateOp") String purchaseDateOp,
                                          @Param("purchaseDate") String purchaseDate,
                                          @Param("stockQtyOp") String stockQtyOp,
                                          @Param("stockQty") String stockQty,
                                          @Param("showHierarchy") Boolean showHierarchy);

    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>" + CTE_PREFIX + SELECT_BODY + WHERE_BODY + ORDER_BODY + "</script>")
    List<ProductPriceVO> selectPriceList(@Param("tenantId") Long tenantId,
                                         @Param("categoryId") Long categoryId,
                                         @Param("keyword") String keyword,
                                         @Param("brand") String brand,
                                         @Param("unitType") String unitType,
                                         @Param("productId") Long productId,
                                         @Param("shelfStatus") Integer shelfStatus,
                                         @Param("purchaseDateOp") String purchaseDateOp,
                                         @Param("purchaseDate") String purchaseDate,
                                         @Param("stockQtyOp") String stockQtyOp,
                                         @Param("stockQty") String stockQty,
                                         @Param("showHierarchy") Boolean showHierarchy);

    /** 品牌下拉：取自商品档案已用品牌 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT DISTINCT p.brand FROM erp_product p WHERE p.deleted = 0 AND p.brand IS NOT NULL AND p.brand <> ''"
        + " AND (CAST(#{tenantId} AS BIGINT) IS NULL OR p.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " ORDER BY p.brand")
    List<String> selectBrands(@Param("tenantId") Long tenantId);

    /** 客户级别下拉：客户档案已用的级别名称 + 客户级别主数据，保持与客户档案同源 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT DISTINCT name FROM ("
        + " SELECT party_level AS name FROM biz_party"
        + "   WHERE deleted = 0 AND party_level IS NOT NULL AND party_level <> ''"
        + "     AND (CAST(#{tenantId} AS BIGINT) IS NULL OR tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " UNION"
        + " SELECT grade_name AS name FROM erp_partner_grade"
        + "   WHERE deleted = 0 AND status = 1 AND grade_name IS NOT NULL AND grade_name <> ''"
        + "     AND (CAST(#{tenantId} AS BIGINT) IS NULL OR tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " ) t ORDER BY name")
    List<String> selectCustomerGrades(@Param("tenantId") Long tenantId);
}
