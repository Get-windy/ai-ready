package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.dto.ProductBarcodeVO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品条码查询 Mapper
 * <p>
 * 商品条码页一行 = 商品 × 单位（主体 erp_product_unit），条码优先取单位行 barcode，
 * 缺失时回退 erp_product_barcode 的默认条码 —— 与既有条码表保持单一口径，不另建重复条码表。
 * 分类过滤走递归 CTE 覆盖所有子分类；「最近采购日期」取状态为已入库(8)/已完成(9)的采购入库单最大单据日期。
 * 可空参数一律以 CAST(#{x} AS 类型) 引用，避免 pg 无法推断裸参数类型。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface ProductBarcodeQueryMapper {

    /** 分类递归 CTE + 最近采购日期聚合 CTE（一次聚合替代逐行相关子查询） */
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
        + ") ";

    String SELECT_BODY = "SELECT"
        + " u.id AS unit_id,"
        + " p.id AS product_id,"
        + " p.image_url AS image_url,"
        + " p.product_name AS product_name,"
        + " p.product_code AS product_code,"
        + " u.unit_name AS unit_name,"
        + " u.conversion_rate AS conversion_rate,"
        + " u.is_base_unit AS is_base_unit,"
        + " (SELECT bu.unit_name FROM erp_product_unit bu"
        + "   WHERE bu.product_id = p.id AND bu.is_base_unit = 1 AND bu.deleted = 0"
        + "   ORDER BY bu.sort_order, bu.id LIMIT 1) AS base_unit_name,"
        + " p.mall_shelf_status AS shelf_status,"
        + " COALESCE(NULLIF(u.barcode, ''), pb.barcode) AS barcode,"
        + " pb.barcode_type AS barcode_type,"
        + " p.spec AS spec,"
        + " p.model AS model,"
        + " p.origin AS origin,"
        + " p.create_time AS create_time,"
        + " p.status AS status,"
        + " pu.last_purchase_date AS last_purchase_date"
        + " FROM erp_product_unit u"
        + " JOIN erp_product p ON p.id = u.product_id AND p.deleted = 0"
        + " LEFT JOIN erp_product_barcode pb ON pb.product_id = u.product_id AND pb.unit_id = u.id"
        + "   AND pb.deleted = 0 AND pb.is_default = 1"
        + " LEFT JOIN purchase pu ON pu.product_id = p.id"
        + " WHERE u.deleted = 0";

    /** 日期比较符（对标下拉：&lt; = &gt; ≠ ≤ ≥）：空值按 ≥ 处理 */
    String CREATE_TIME_OP_BODY =
        " CASE COALESCE(NULLIF(CAST(#{createTimeOp} AS VARCHAR), ''), '&gt;=')"
        + "   WHEN '&lt;'  THEN p.create_time &lt; CAST(CAST(#{createTimeStart} AS VARCHAR) AS TIMESTAMP)"
        + "   WHEN '&gt;' THEN p.create_time &gt;= CAST(CAST(#{createTimeStart} AS VARCHAR) AS TIMESTAMP) + INTERVAL '1 day'"
        + "   WHEN '&lt;=' THEN p.create_time &lt; CAST(CAST(#{createTimeStart} AS VARCHAR) AS TIMESTAMP) + INTERVAL '1 day'"
        + "   WHEN '='  THEN p.create_time &gt;= CAST(CAST(#{createTimeStart} AS VARCHAR) AS TIMESTAMP)"
        + "                AND p.create_time &lt; CAST(CAST(#{createTimeStart} AS VARCHAR) AS TIMESTAMP) + INTERVAL '1 day'"
        + "   WHEN '!=' THEN (p.create_time &lt; CAST(CAST(#{createTimeStart} AS VARCHAR) AS TIMESTAMP)"
        + "                OR p.create_time &gt;= CAST(CAST(#{createTimeStart} AS VARCHAR) AS TIMESTAMP) + INTERVAL '1 day')"
        + "   ELSE p.create_time &gt;= CAST(CAST(#{createTimeStart} AS VARCHAR) AS TIMESTAMP)"
        + " END";

    String PURCHASE_DATE_OP_BODY =
        " CASE COALESCE(NULLIF(CAST(#{purchaseDateOp} AS VARCHAR), ''), '&gt;=')"
        + "   WHEN '&lt;'  THEN pu.last_purchase_date &lt; CAST(CAST(#{purchaseDateStart} AS VARCHAR) AS DATE)"
        + "   WHEN '&gt;' THEN pu.last_purchase_date &gt; CAST(CAST(#{purchaseDateStart} AS VARCHAR) AS DATE)"
        + "   WHEN '&lt;=' THEN pu.last_purchase_date &lt;= CAST(CAST(#{purchaseDateStart} AS VARCHAR) AS DATE)"
        + "   WHEN '='  THEN pu.last_purchase_date = CAST(CAST(#{purchaseDateStart} AS VARCHAR) AS DATE)"
        + "   WHEN '!=' THEN pu.last_purchase_date &lt;&gt; CAST(CAST(#{purchaseDateStart} AS VARCHAR) AS DATE)"
        + "   ELSE pu.last_purchase_date &gt;= CAST(CAST(#{purchaseDateStart} AS VARCHAR) AS DATE)"
        + " END";

    String WHERE_BODY = " AND (CAST(#{tenantId} AS BIGINT) IS NULL OR u.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " AND (CAST(#{categoryId} AS BIGINT) IS NULL OR p.category_id IN (SELECT id FROM category_scope))"
        + " AND (CAST(#{keyword} AS VARCHAR) IS NULL OR CAST(#{keyword} AS VARCHAR) = ''"
        + "   OR p.product_name ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%'"
        + "   OR p.product_code ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%'"
        + "   OR COALESCE(NULLIF(u.barcode, ''), pb.barcode) ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%'"
        + "   OR u.unit_name ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%')"
        + " AND (CAST(#{shelfStatus} AS INTEGER) IS NULL OR p.mall_shelf_status = CAST(#{shelfStatus} AS INTEGER))"
        + " AND (CAST(#{status} AS VARCHAR) IS NULL OR CAST(#{status} AS VARCHAR) = ''"
        + "   OR p.status = CAST(#{status} AS VARCHAR))"
        + " AND (CAST(#{createTimeStart} AS VARCHAR) IS NULL OR CAST(#{createTimeStart} AS VARCHAR) = ''"
        + "   OR (" + CREATE_TIME_OP_BODY + "))"
        + " AND (CAST(#{barcodeFilter} AS VARCHAR) IS NULL OR CAST(#{barcodeFilter} AS VARCHAR) = ''"
        + "   OR CAST(#{barcodeFilter} AS VARCHAR) = 'ALL'"
        + "   OR (CAST(#{barcodeFilter} AS VARCHAR) = 'HAS'"
        + "     AND COALESCE(NULLIF(u.barcode, ''), pb.barcode) IS NOT NULL"
        + "     AND COALESCE(NULLIF(u.barcode, ''), pb.barcode) &lt;&gt; '')"
        + "   OR (CAST(#{barcodeFilter} AS VARCHAR) = 'NONE'"
        + "     AND (COALESCE(NULLIF(u.barcode, ''), pb.barcode) IS NULL"
        + "       OR COALESCE(NULLIF(u.barcode, ''), pb.barcode) = ''))"
        + "   OR (CAST(#{barcodeFilter} AS VARCHAR) NOT IN ('HAS', 'NONE')"
        + "     AND COALESCE(pb.barcode_type, 'OTHER') = CAST(#{barcodeFilter} AS VARCHAR)))"
        + " AND (CAST(#{purchaseDateStart} AS VARCHAR) IS NULL OR CAST(#{purchaseDateStart} AS VARCHAR) = ''"
        + "   OR (" + PURCHASE_DATE_OP_BODY + "))";

    String ORDER_BODY = " ORDER BY p.create_time DESC, p.id, u.sort_order, u.id";

    /**
     * 表头排序（对标 ql361：商品名称/货号/条码可排序）。
     * <p>
     * 由 Service 以**白名单**方式生成整段 ORDER BY 后再以 ${} 注入，字段名不来自请求原文，避免 SQL 注入。
     */
    default String orderBodyOf(String sortField, String sortOrder) {
        String col = switch (sortField == null ? "" : sortField) {
            case "productName" -> "p.product_name";
            case "productCode" -> "p.product_code";
            case "barcode" -> "COALESCE(NULLIF(u.barcode, ''), pb.barcode)";
            default -> null;
        };
        if (col == null) {
            return ORDER_BODY;
        }
        String dir = "desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";
        return " ORDER BY " + col + " " + dir + " NULLS LAST, p.create_time DESC, p.id, u.sort_order, u.id";
    }

    /**
     * 分页查询商品条码（商品 × 单位）
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>" + CTE_PREFIX + SELECT_BODY + WHERE_BODY + "${orderBody}" + "</script>")
    IPage<ProductBarcodeVO> selectBarcodePage(Page<ProductBarcodeVO> page,
                                              @Param("tenantId") Long tenantId,
                                              @Param("categoryId") Long categoryId,
                                              @Param("keyword") String keyword,
                                              @Param("barcodeFilter") String barcodeFilter,
                                              @Param("shelfStatus") Integer shelfStatus,
                                              @Param("status") String status,
                                              @Param("createTimeOp") String createTimeOp,
                                              @Param("createTimeStart") String createTimeStart,
                                              @Param("purchaseDateOp") String purchaseDateOp,
                                              @Param("purchaseDateStart") String purchaseDateStart,
                                              @Param("orderBody") String orderBody);

    /**
     * 导出用：查询全部商品条码行（不分页）
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>" + CTE_PREFIX + SELECT_BODY + WHERE_BODY + "${orderBody}" + "</script>")
    List<ProductBarcodeVO> selectBarcodeList(@Param("tenantId") Long tenantId,
                                             @Param("categoryId") Long categoryId,
                                             @Param("keyword") String keyword,
                                             @Param("barcodeFilter") String barcodeFilter,
                                             @Param("shelfStatus") Integer shelfStatus,
                                             @Param("status") String status,
                                             @Param("createTimeOp") String createTimeOp,
                                             @Param("createTimeStart") String createTimeStart,
                                             @Param("purchaseDateOp") String purchaseDateOp,
                                             @Param("purchaseDateStart") String purchaseDateStart,
                                             @Param("orderBody") String orderBody);
}
