package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.controller.initial.InitialStockVO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 期初库存查询 Mapper（库存期初页 / 导出共用同一份 SQL）
 * <p>
 * 单一数据主体是 {@code erp_stock} 中 {@code is_initial = 1} 的行（期初台账），
 * 通过 {@code product_id} 关联 {@code erp_product} 取对标要求的展示列：
 * 条码 / 规格 / 型号 / 产地 / 小单位（本系统 erp_stock 表没有这些列，故不新增冗余列，
 * 走「关联商品档案 + 后端算金额」的口径 —— 见《库存期初开发文档》§7.4 路线 A）。
 * <p>
 * 期初金额 {@code quantity * unit_price} 为派生值，**不落库**（避免数量/单价改了金额没改）。
 * 可空参数一律以 {@code CAST(#{x} AS 类型)} 引用，避免 PostgreSQL 无法推断裸参数类型。
 * <p>
 * ⚠️ 本 Mapper 走 {@code @InterceptorIgnore(tenantLine = "true")} 关闭自动租户注入
 * （手写 JOIN 由本 SQL 自己拼 tenant_id 条件，避免拦截器对 JOIN 两侧表重复注入）。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface InitialStockQueryMapper {

    /** 分类范围递归 CTE：传入分类ID时覆盖其全部子分类；传空时不产生任何分类范围 */
    String CATEGORY_CTE = "WITH RECURSIVE category_scope AS ("
        + " SELECT id FROM erp_product_category"
        + "  WHERE deleted = 0 AND id = CAST(#{categoryId} AS BIGINT)"
        + "    AND (CAST(#{tenantId} AS BIGINT) IS NULL OR tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " UNION ALL"
        + " SELECT c.id FROM erp_product_category c JOIN category_scope cs ON c.parent_id = cs.id"
        + "  WHERE c.deleted = 0"
        + "    AND (CAST(#{tenantId} AS BIGINT) IS NULL OR c.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + ") ";

    /** 商品名称/货号取库存行快照，快照为空时回退商品档案 */
    String PRODUCT_NAME_EXPR = "COALESCE(NULLIF(s.product_name, ''), p.product_name)";
    String PRODUCT_CODE_EXPR = "COALESCE(NULLIF(s.product_code, ''), p.product_code)";

    String SELECT_BODY = "SELECT"
        + " s.id AS id,"
        + " COALESCE(s.product_id, p.id) AS product_id,"
        + " " + PRODUCT_NAME_EXPR + " AS product_name,"
        + " " + PRODUCT_CODE_EXPR + " AS product_code,"
        + " p.barcode AS barcode,"
        + " p.spec AS spec,"
        + " p.model AS model,"
        + " p.origin AS origin,"
        // 小单位：优先商品档案的小单位（对标列名即「小单位」），其次入库时的单位快照，最后回退基础单位
        + " COALESCE(NULLIF(p.small_unit, ''), NULLIF(s.unit, ''), p.unit) AS unit,"
        + " s.warehouse_id AS warehouse_id,"
        + " s.warehouse_name AS warehouse_name,"
        + " s.quantity AS quantity,"
        + " s.unit_price AS unit_price,"
        + " (s.quantity * s.unit_price) AS amount,"
        + " s.production_date AS production_date,"
        + " s.validity_date AS validity_date,"
        + " s.remark AS remark,"
        + " s.create_time AS create_time"
        + " FROM erp_stock s"
        + " LEFT JOIN erp_product p ON p.id = s.product_id AND p.deleted = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR p.tenant_id = CAST(#{tenantId} AS BIGINT))";

    String WHERE_BODY = " WHERE s.deleted = 0 AND s.is_initial = 1"
        + " AND (CAST(#{tenantId} AS BIGINT) IS NULL OR s.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " AND (CAST(#{warehouseId} AS BIGINT) IS NULL OR s.warehouse_id = CAST(#{warehouseId} AS BIGINT))"
        + " AND (CAST(#{categoryId} AS BIGINT) IS NULL OR p.category_id IN (SELECT id FROM category_scope))"
        + " AND (CAST(#{keyword} AS VARCHAR) IS NULL OR CAST(#{keyword} AS VARCHAR) = ''"
        + "   OR " + PRODUCT_NAME_EXPR + " ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%'"
        + "   OR " + PRODUCT_CODE_EXPR + " ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%'"
        + "   OR p.barcode ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%')"
        + " AND (CAST(#{productCode} AS VARCHAR) IS NULL OR CAST(#{productCode} AS VARCHAR) = ''"
        + "   OR " + PRODUCT_CODE_EXPR + " ILIKE '%' || CAST(#{productCode} AS VARCHAR) || '%')"
        + " AND (CAST(#{productName} AS VARCHAR) IS NULL OR CAST(#{productName} AS VARCHAR) = ''"
        + "   OR " + PRODUCT_NAME_EXPR + " ILIKE '%' || CAST(#{productName} AS VARCHAR) || '%')"
        // 期初数量筛选项（对标 ql361「期初数量」下拉）：ALL=全部 / HAS=有期初 / NONE=无期初
        + " AND (CAST(#{initialQtyFilter} AS VARCHAR) IS NULL OR CAST(#{initialQtyFilter} AS VARCHAR) = ''"
        + "   OR CAST(#{initialQtyFilter} AS VARCHAR) = 'ALL'"
        + "   OR (CAST(#{initialQtyFilter} AS VARCHAR) = 'HAS' AND s.quantity IS NOT NULL AND s.quantity <> 0)"
        + "   OR (CAST(#{initialQtyFilter} AS VARCHAR) = 'NONE' AND (s.quantity IS NULL OR s.quantity = 0)))";

    String ORDER_BODY = " ORDER BY s.id DESC";

    /**
     * 分页查询期初库存行
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select(CATEGORY_CTE + SELECT_BODY + WHERE_BODY + ORDER_BODY)
    IPage<InitialStockVO> selectInitialStockPage(Page<InitialStockVO> page,
                                                 @Param("tenantId") Long tenantId,
                                                 @Param("warehouseId") Long warehouseId,
                                                 @Param("categoryId") Long categoryId,
                                                 @Param("keyword") String keyword,
                                                 @Param("productCode") String productCode,
                                                 @Param("productName") String productName,
                                                 @Param("initialQtyFilter") String initialQtyFilter);

    /**
     * 导出用：查询全部期初库存行（不分页，与分页同口径以保证「导出条数 = 列表总数」）
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select(CATEGORY_CTE + SELECT_BODY + WHERE_BODY + ORDER_BODY)
    List<InitialStockVO> selectInitialStockList(@Param("tenantId") Long tenantId,
                                                @Param("warehouseId") Long warehouseId,
                                                @Param("categoryId") Long categoryId,
                                                @Param("keyword") String keyword,
                                                @Param("productCode") String productCode,
                                                @Param("productName") String productName,
                                                @Param("initialQtyFilter") String initialQtyFilter);
}
