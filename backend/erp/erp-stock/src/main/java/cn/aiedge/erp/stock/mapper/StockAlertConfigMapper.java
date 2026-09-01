package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.dto.StockAlertQueryVO;
import cn.aiedge.erp.stock.entity.StockAlertConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StockAlertConfigMapper extends BaseMapper<StockAlertConfig> {

    @Select("SELECT * FROM erp_stock_alert_config WHERE product_id = #{productId} AND warehouse_id = #{warehouseId} AND active = 1 AND deleted = 0")
    StockAlertConfig selectByProductAndWarehouse(@Param("productId") Long productId, @Param("warehouseId") Long warehouseId);

    @Select("SELECT * FROM erp_stock_alert_config WHERE warehouse_id = #{warehouseId} AND active = 1 AND deleted = 0")
    List<StockAlertConfig> selectByWarehouse(@Param("warehouseId") Long warehouseId);

    @Select("SELECT * FROM erp_stock_alert_config WHERE active = 1 AND deleted = 0")
    List<StockAlertConfig> selectAllActive();

    @Select("SELECT COUNT(*) FROM erp_stock_alert_config WHERE active = 1 AND deleted = 0 AND tenant_id = #{tenantId}")
    Integer countActive(@Param("tenantId") Long tenantId);

    /**
     * 预警设置页（库存预警固定值设置）——商品清单 + 当前仓库下的上下限配置。
     * <p>
     * warehouseId 指定时：以商品主数据为主体 LEFT JOIN 该仓库配置（未配置商品也展示，供行内新建）；
     * warehouseId 为空（全部仓库）：以配置表为主体 JOIN 商品（展示所有已配置的「商品×仓库」行）。
     * 小单位取 erp_product_unit 中 unit_type='SMALL' 的换算单位；口味取商品主数据 new 列 taste。
     *
     * @author AI-Ready Team
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>"
        + "WITH category_scope AS ("
        + " SELECT id FROM erp_product_category"
        + " WHERE CAST(#{categoryId} AS BIGINT) IS NOT NULL AND id = CAST(#{categoryId} AS BIGINT)"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " UNION ALL"
        + " SELECT c.id FROM erp_product_category c JOIN category_scope cs ON c.parent_id = cs.id"
        + " WHERE CAST(#{tenantId} AS BIGINT) IS NULL OR c.tenant_id = CAST(#{tenantId} AS BIGINT)"
        + "), small_unit_scope AS ("
        + " SELECT product_id, unit_name FROM ("
        + "  SELECT DISTINCT ON (product_id) product_id, unit_name"
        + "  FROM erp_product_unit"
        + "  WHERE unit_type = 'SMALL' AND deleted = 0"
        + "  ORDER BY product_id, sort_order"
        + " ) su"
        + ")"
        + "SELECT"
        + " c.id AS id,"
        + " CAST(COALESCE(p.id, c.product_id) AS BIGINT) AS product_id,"
        + " COALESCE(NULLIF(p.product_code_alias, ''), p.product_code, c.product_code) AS product_code,"
        + " COALESCE(p.product_name, c.product_name) AS product_name,"
        + " p.taste AS taste,"
        + " p.model AS model,"
        + " p.unit AS unit,"
        + " COALESCE(p.small_unit, su.unit_name) AS small_unit,"
        + " p.barcode AS barcode,"
        + " p.spec AS spec,"
        + " p.origin AS origin,"
        + " p.brand AS brand,"
        + " c.warehouse_id AS warehouse_id,"
        + " COALESCE(w.warehouse_name, c.warehouse_name) AS warehouse_name,"
        + " c.max_stock AS max_stock,"
        + " c.min_stock AS min_stock,"
        + " c.safety_stock AS safety_stock,"
        + " c.active AS active"
        + " FROM "
        + " <choose>"
        + "  <when test='warehouseId != null'>"
        + "   erp_product p"
        + "   LEFT JOIN erp_stock_alert_config c ON c.product_id = p.id AND c.warehouse_id = CAST(#{warehouseId} AS BIGINT) AND c.deleted = 0"
        + "   LEFT JOIN erp_warehouse w ON w.id = CAST(#{warehouseId} AS BIGINT)"
        + "  </when>"
        + "  <otherwise>"
        + "   erp_stock_alert_config c"
        + "   JOIN erp_product p ON p.id = c.product_id AND p.deleted = 0"
        + "   LEFT JOIN erp_warehouse w ON w.id = c.warehouse_id"
        + "  </otherwise>"
        + " </choose>"
        + " LEFT JOIN small_unit_scope su ON su.product_id = CAST(COALESCE(p.id, c.product_id) AS BIGINT)"
        + " WHERE p.deleted = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR CAST(COALESCE(p.tenant_id, c.tenant_id) AS BIGINT) = CAST(#{tenantId} AS BIGINT))"
        + "   <choose>"
        + "    <when test='warehouseId != null'>"
        + "     AND COALESCE(c.deleted, 0) = 0"
        + "    </when>"
        + "    <otherwise>"
        + "     AND c.deleted = 0 AND c.active = 1"
        + "    </otherwise>"
        + "   </choose>"
        + "   AND (CAST(#{keyword} AS VARCHAR) IS NULL OR CAST(#{keyword} AS VARCHAR) = ''"
        + "     OR COALESCE(p.product_name, '') ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%'"
        + "     OR COALESCE(p.product_code, '') ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%'"
        + "     OR COALESCE(p.product_code_alias, '') ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%'"
        + "     OR COALESCE(p.barcode, '') ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%')"
        + "   AND (CAST(#{brand} AS VARCHAR) IS NULL OR CAST(#{brand} AS VARCHAR) = ''"
        + "     OR COALESCE(p.brand, '') ILIKE '%' || CAST(#{brand} AS VARCHAR) || '%')"
        + "   AND (CAST(#{categoryId} AS BIGINT) IS NULL OR CAST(COALESCE(p.category_id, 0) AS BIGINT) IN (SELECT id FROM category_scope))"
        + " ORDER BY p.product_name, c.warehouse_id NULLS LAST"
        + "</script>")
    IPage<StockAlertQueryVO> configItemsPage(Page<StockAlertQueryVO> page,
                                             @Param("tenantId") Long tenantId,
                                             @Param("warehouseId") Long warehouseId,
                                             @Param("keyword") String keyword,
                                             @Param("brand") String brand,
                                             @Param("categoryId") Long categoryId);
}