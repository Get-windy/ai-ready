package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.dto.ProductLocationVO;
import cn.aiedge.erp.stock.entity.ProductLocation;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品推荐货位 Mapper
 * <p>
 * 列表主体为商品（erp_product），按所选仓库 LEFT JOIN 商品-推荐货位绑定（erp_product_location），
 * 因此「未设置货位」的商品同样成行，便于批量设置。
 * 货位主数据为全局唯一口径 wms_location（本查询只读其 location_code 校验），不复制货位属性；
 * 库存口径取 erp_stock（商品 × 仓库）汇总；等级价取 erp_product_grade_price 按等级聚合。
 * 可空参数一律用 CAST(#{x} AS 类型) 引用，避免 PG 无法推断裸参数类型。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface ProductLocationMapper extends BaseMapper<ProductLocation> {

    /** 分类递归 CTE + 所选仓库库存聚合 CTE + 价格等级聚合 CTE */
    String CTE_PREFIX = "WITH RECURSIVE category_scope AS ("
        + " SELECT id FROM erp_product_category"
        + " WHERE CAST(#{categoryId} AS BIGINT) IS NOT NULL AND id = CAST(#{categoryId} AS BIGINT)"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " UNION ALL"
        + " SELECT c.id FROM erp_product_category c JOIN category_scope cs ON c.parent_id = cs.id"
        + " WHERE CAST(#{tenantId} AS BIGINT) IS NULL OR c.tenant_id = CAST(#{tenantId} AS BIGINT)"
        + "), stock_agg AS ("
        + " SELECT product_id, SUM(COALESCE(quantity, 0)) AS qty"
        + " FROM erp_stock"
        + " WHERE deleted = 0"
        + "   AND (CAST(#{warehouseId} AS BIGINT) IS NULL OR warehouse_id = CAST(#{warehouseId} AS BIGINT))"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " GROUP BY product_id"
        + "), pos_agg AS ("
        // 仓库未选（全部仓库）时按商品聚合：仓库名/货位编码以逗号拼接；指定仓库时至多一条绑定，聚合结果即该条
        + " SELECT pl.product_id,"
        + "   MIN(pl.id) AS bind_id,"
        + "   MIN(pl.warehouse_id) AS bind_warehouse_id,"
        + "   MIN(pl.location_id) AS bind_location_id,"
        + "   STRING_AGG(pl.location_code, ',' ORDER BY pl.warehouse_id) AS bind_location_code,"
        + "   STRING_AGG(w.warehouse_name, ',' ORDER BY pl.warehouse_id) AS bind_warehouse_name,"
        + "   MAX(pl.remark) AS bind_remark,"
        + "   MAX(pl.update_time) AS bind_update_time"
        + " FROM erp_product_location pl"
        + " LEFT JOIN erp_warehouse w ON w.id = pl.warehouse_id"
        + " WHERE pl.deleted = 0"
        + "   AND (CAST(#{warehouseId} AS BIGINT) IS NULL OR pl.warehouse_id = CAST(#{warehouseId} AS BIGINT))"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR pl.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " GROUP BY pl.product_id"
        + "), grade_agg AS ("
        + " SELECT product_id,"
        + "   MAX(CASE WHEN grade_code = 'GRADE_1' THEN grade_price END) AS gp1,"
        + "   MAX(CASE WHEN grade_code = 'GRADE_2' THEN grade_price END) AS gp2,"
        + "   MAX(CASE WHEN grade_code = 'GRADE_3' THEN grade_price END) AS gp3,"
        + "   MAX(CASE WHEN grade_code = 'GRADE_4' THEN grade_price END) AS gp4,"
        + "   MAX(CASE WHEN grade_code = 'GRADE_5' THEN grade_price END) AS gp5,"
        + "   MAX(CASE WHEN grade_code = 'GRADE_6' THEN grade_price END) AS gp6,"
        + "   MAX(CASE WHEN grade_code = 'GRADE_7' THEN grade_price END) AS gp7,"
        + "   MAX(CASE WHEN grade_code = 'GRADE_8' THEN grade_price END) AS gp8"
        + " FROM erp_product_grade_price"
        + " WHERE deleted = 0 AND COALESCE(is_active, 1) = 1"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " GROUP BY product_id"
        + ") ";

    String SELECT_BODY = "SELECT"
        + " po.bind_id AS id,"
        + " p.id AS product_id,"
        + " p.product_name AS product_name,"
        + " COALESCE(NULLIF(p.product_code_alias, ''), p.product_code) AS product_code,"
        + " p.image_url AS image_url,"
        + " p.mall_shelf_status AS shelf_status,"
        + " p.unit AS unit,"
        + " p.barcode AS barcode,"
        + " p.spec AS spec,"
        + " p.model AS model,"
        + " p.origin AS origin,"
        + " p.brand AS brand,"
        + " po.bind_warehouse_id AS warehouse_id,"
        + " po.bind_warehouse_name AS warehouse_name,"
        + " po.bind_location_id AS location_id,"
        + " po.bind_location_code AS location_code,"
        + " p.retail_price AS retail_price,"
        + " p.wholesale_price AS wholesale_price,"
        + " ga.gp1 AS grade_price1, ga.gp2 AS grade_price2, ga.gp3 AS grade_price3, ga.gp4 AS grade_price4,"
        + " ga.gp5 AS grade_price5, ga.gp6 AS grade_price6, ga.gp7 AS grade_price7, ga.gp8 AS grade_price8,"
        + " po.bind_remark AS remark,"
        + " COALESCE(po.bind_update_time, p.update_time) AS modify_time"
        + " FROM erp_product p"
        + " LEFT JOIN pos_agg po ON po.product_id = p.id"
        + " LEFT JOIN stock_agg st ON st.product_id = p.id"
        + " LEFT JOIN grade_agg ga ON ga.product_id = p.id";

    String WHERE_BODY = " WHERE p.deleted = 0"
        + " AND (CAST(#{tenantId} AS BIGINT) IS NULL OR p.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " AND (CAST(#{categoryId} AS BIGINT) IS NULL OR p.category_id IN (SELECT id FROM category_scope))"
        + " AND (CAST(#{keyword} AS VARCHAR) IS NULL OR p.product_name LIKE CONCAT('%', CAST(#{keyword} AS VARCHAR), '%')"
        + "      OR p.product_code LIKE CONCAT('%', CAST(#{keyword} AS VARCHAR), '%')"
        + "      OR p.product_code_alias LIKE CONCAT('%', CAST(#{keyword} AS VARCHAR), '%'))"
        + " AND (CAST(#{brand} AS VARCHAR) IS NULL OR p.brand LIKE CONCAT('%', CAST(#{brand} AS VARCHAR), '%'))"
        + " AND (CAST(#{locationCode} AS VARCHAR) IS NULL OR po.bind_location_code LIKE CONCAT('%', CAST(#{locationCode} AS VARCHAR), '%'))"
        + " AND (CAST(#{hasBarcodeStatus} AS INTEGER) IS NULL"
        + "      OR (CAST(#{hasBarcodeStatus} AS INTEGER) = 1 AND COALESCE(p.barcode, '') = '')"
        + "      OR (CAST(#{hasBarcodeStatus} AS INTEGER) = 2 AND COALESCE(p.barcode, '') != ''))"
        + " AND (CAST(#{shelfStatus} AS INTEGER) IS NULL OR p.mall_shelf_status = CAST(#{shelfStatus} AS INTEGER))"
        + " AND (CAST(#{showStop} AS INTEGER) IS NULL"
        + "      OR (CAST(#{showStop} AS INTEGER) = 2 AND p.status = 'ENABLED')"
        + "      OR (CAST(#{showStop} AS INTEGER) = 1 AND p.status = 'DISABLED'))"
        + " AND (CAST(#{onlyUnsettedGoods} AS BOOLEAN) IS NOT TRUE OR po.bind_id IS NULL)"
        + " AND (CAST(#{onlyStockGoods} AS BOOLEAN) IS NOT TRUE OR COALESCE(st.qty, 0) > 0)";

    String ORDER_BODY = " ORDER BY p.id DESC";

    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>" + CTE_PREFIX + SELECT_BODY + WHERE_BODY + ORDER_BODY + "</script>")
    IPage<ProductLocationVO> selectProductLocationPage(Page<ProductLocationVO> page,
                                                       @Param("tenantId") Long tenantId,
                                                       @Param("warehouseId") Long warehouseId,
                                                       @Param("categoryId") Long categoryId,
                                                       @Param("keyword") String keyword,
                                                       @Param("brand") String brand,
                                                       @Param("locationCode") String locationCode,
                                                       @Param("hasBarcodeStatus") Integer hasBarcodeStatus,
                                                       @Param("shelfStatus") Integer shelfStatus,
                                                       @Param("showStop") Integer showStop,
                                                       @Param("onlyUnsettedGoods") Boolean onlyUnsettedGoods,
                                                       @Param("onlyStockGoods") Boolean onlyStockGoods);

    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>" + CTE_PREFIX + SELECT_BODY + WHERE_BODY + ORDER_BODY + "</script>")
    List<ProductLocationVO> selectProductLocationList(@Param("tenantId") Long tenantId,
                                                      @Param("warehouseId") Long warehouseId,
                                                      @Param("categoryId") Long categoryId,
                                                      @Param("keyword") String keyword,
                                                      @Param("brand") String brand,
                                                      @Param("locationCode") String locationCode,
                                                      @Param("hasBarcodeStatus") Integer hasBarcodeStatus,
                                                      @Param("shelfStatus") Integer shelfStatus,
                                                      @Param("showStop") Integer showStop,
                                                      @Param("onlyUnsettedGoods") Boolean onlyUnsettedGoods,
                                                      @Param("onlyStockGoods") Boolean onlyStockGoods);

    /**
     * 校验货位并取回货位编码（货位为全局基础数据 wms_location，只读不复制）
     *
     * @return 货位编码；货位不存在或不属于该仓库时返回 null
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT location_code FROM wms_location"
        + " WHERE id = #{locationId} AND warehouse_id = #{warehouseId} AND deleted = 0")
    String selectLocationCode(@Param("locationId") Long locationId, @Param("warehouseId") Long warehouseId);
}
