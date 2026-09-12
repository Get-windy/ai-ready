package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.dto.GradePriceVO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 级别指定价（子标签 3）/ 客户指定价（子标签 4）查询 Mapper
 * <p>
 * 规则行只存「级别或客户 + 商品(或分类) + 单位 + 价格规则」；商品档案侧字段（条码/规格/型号/品牌/
 * 预设进价/零售价/批发价/8 个价格等级）查询时实时取自 erp_product / erp_product_unit，
 * 保证同一价格口径在商品档案、批量改价、指定价三处完全一致。
 */
@Mapper
public interface GradePriceQueryMapper {

    /** 商品档案侧字段（别名固定，两个查询共用） */
    String PRODUCT_COLUMNS =
        " COALESCE(u.barcode, p.barcode) AS barcode,"
        + " p.spec AS spec,"
        + " p.model AS model,"
        + " p.brand AS brand,"
        + " u.preset_purchase_price AS preset_purchase_price,"
        + " u.retail_price AS retail_price,"
        + " u.wholesale_price AS wholesale_price,"
        + " u.grade_price_1 AS gradePrice1,"
        + " u.grade_price_2 AS gradePrice2,"
        + " u.grade_price_3 AS gradePrice3,"
        + " u.grade_price_4 AS gradePrice4,"
        + " u.grade_price_5 AS gradePrice5,"
        + " u.grade_price_6 AS gradePrice6,"
        + " u.grade_price_7 AS gradePrice7,"
        + " u.grade_price_8 AS gradePrice8,"
        + " t.update_time AS last_modify_time,"
        + " COALESCE(su.real_name, su.nickname, su.username) AS last_modifier_name";

    String PRODUCT_JOINS =
        " LEFT JOIN erp_product p ON p.id = t.product_id AND p.deleted = 0"
        // 未指定单位时回退到商品基础单位，保证价格列始终有口径
        + " LEFT JOIN erp_product_unit u ON u.deleted = 0 AND u.id = COALESCE(t.unit_id,"
        + "   (SELECT bu.id FROM erp_product_unit bu WHERE bu.product_id = t.product_id AND bu.is_base_unit = 1"
        + "     AND bu.deleted = 0 ORDER BY bu.sort_order, bu.id LIMIT 1))"
        + " LEFT JOIN sys_user su ON su.id = t.update_by AND su.deleted = 0";

    // ───────────────────────── 子标签 3：级别指定价 ─────────────────────────

    String LEVEL_WHERE =
        " WHERE t.deleted = 0"
        + " AND (CAST(#{tenantId} AS BIGINT) IS NULL OR t.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " AND (CAST(#{gradeName} AS VARCHAR) IS NULL OR CAST(#{gradeName} AS VARCHAR) = ''"
        + "   OR t.grade_name = CAST(#{gradeName} AS VARCHAR))"
        + " AND (CAST(#{keyword} AS VARCHAR) IS NULL OR CAST(#{keyword} AS VARCHAR) = ''"
        + "   OR COALESCE(t.product_name, t.category_name) ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%'"
        + "   OR t.product_code ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%')"
        + " AND (CAST(#{brand} AS VARCHAR) IS NULL OR CAST(#{brand} AS VARCHAR) = ''"
        + "   OR p.brand = CAST(#{brand} AS VARCHAR))";

    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>SELECT t.id, t.grade_id AS grade_id, t.grade_name AS grade_name,"
        + " t.product_id AS product_id, t.product_code AS product_code, t.product_name AS product_name,"
        + " t.category_id AS category_id, t.category_name AS category_name,"
        + " COALESCE(t.product_name, t.category_name) AS target_name,"
        + " t.unit_id AS unit_id, t.unit_name AS unit_name, t.price_rule AS price_rule,"
        + " t.base_price_type AS base_price_type, t.calc_operator AS calc_operator, t.calc_value AS calc_value,"
        + PRODUCT_COLUMNS
        + " FROM erp_customer_grade_price t" + PRODUCT_JOINS + LEVEL_WHERE
        + " ORDER BY t.update_time DESC, t.id DESC</script>")
    IPage<GradePriceVO> selectLevelPricePage(Page<GradePriceVO> page,
                                             @Param("tenantId") Long tenantId,
                                             @Param("gradeName") String gradeName,
                                             @Param("keyword") String keyword,
                                             @Param("brand") String brand);

    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>SELECT t.id, t.grade_id AS grade_id, t.grade_name AS grade_name,"
        + " t.product_id AS product_id, t.product_code AS product_code, t.product_name AS product_name,"
        + " t.category_id AS category_id, t.category_name AS category_name,"
        + " COALESCE(t.product_name, t.category_name) AS target_name,"
        + " t.unit_id AS unit_id, t.unit_name AS unit_name, t.price_rule AS price_rule,"
        + " t.base_price_type AS base_price_type, t.calc_operator AS calc_operator, t.calc_value AS calc_value,"
        + PRODUCT_COLUMNS
        + " FROM erp_customer_grade_price t" + PRODUCT_JOINS + LEVEL_WHERE
        + " ORDER BY t.update_time DESC, t.id DESC</script>")
    List<GradePriceVO> selectLevelPriceList(@Param("tenantId") Long tenantId,
                                            @Param("gradeName") String gradeName,
                                            @Param("keyword") String keyword,
                                            @Param("brand") String brand);

    // ───────────────────────── 子标签 4：客户指定价 ─────────────────────────

    String CUSTOMER_WHERE =
        " WHERE t.deleted = 0"
        + " AND (CAST(#{tenantId} AS BIGINT) IS NULL OR t.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " AND (CAST(#{customerId} AS BIGINT) IS NULL OR t.customer_id = CAST(#{customerId} AS BIGINT))"
        + " AND (CAST(#{productId} AS BIGINT) IS NULL OR t.product_id = CAST(#{productId} AS BIGINT))"
        + " AND (CAST(#{keyword} AS VARCHAR) IS NULL OR CAST(#{keyword} AS VARCHAR) = ''"
        + "   OR COALESCE(t.product_name, t.category_name) ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%'"
        + "   OR t.product_code ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%')"
        + " AND (CAST(#{brand} AS VARCHAR) IS NULL OR CAST(#{brand} AS VARCHAR) = ''"
        + "   OR p.brand = CAST(#{brand} AS VARCHAR))";

    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>SELECT t.id, t.customer_id AS customer_id,"
        + " bp.party_name AS customer_name, bp.party_code AS customer_code,"
        + " t.product_id AS product_id, t.product_code AS product_code, t.product_name AS product_name,"
        + " t.category_id AS category_id, t.category_name AS category_name,"
        + " COALESCE(t.product_name, t.category_name) AS target_name,"
        + " t.unit_id AS unit_id, t.unit_name AS unit_name, t.price_rule AS price_rule,"
        + " t.price AS price, t.price_type AS price_type,"
        + " t.base_price_type AS base_price_type, t.calc_operator AS calc_operator, t.calc_value AS calc_value,"
        + PRODUCT_COLUMNS
        + " FROM erp_customer_product_price t"
        + " LEFT JOIN biz_party bp ON bp.id = t.customer_id AND bp.deleted = 0"
        + PRODUCT_JOINS + CUSTOMER_WHERE
        + " ORDER BY t.update_time DESC, t.id DESC</script>")
    IPage<GradePriceVO> selectCustomerPricePage(Page<GradePriceVO> page,
                                                @Param("tenantId") Long tenantId,
                                                @Param("customerId") Long customerId,
                                                @Param("productId") Long productId,
                                                @Param("keyword") String keyword,
                                                @Param("brand") String brand);

    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>SELECT t.id, t.customer_id AS customer_id,"
        + " bp.party_name AS customer_name, bp.party_code AS customer_code,"
        + " t.product_id AS product_id, t.product_code AS product_code, t.product_name AS product_name,"
        + " t.category_id AS category_id, t.category_name AS category_name,"
        + " COALESCE(t.product_name, t.category_name) AS target_name,"
        + " t.unit_id AS unit_id, t.unit_name AS unit_name, t.price_rule AS price_rule,"
        + " t.price AS price, t.price_type AS price_type,"
        + " t.base_price_type AS base_price_type, t.calc_operator AS calc_operator, t.calc_value AS calc_value,"
        + PRODUCT_COLUMNS
        + " FROM erp_customer_product_price t"
        + " LEFT JOIN biz_party bp ON bp.id = t.customer_id AND bp.deleted = 0"
        + PRODUCT_JOINS + CUSTOMER_WHERE
        + " ORDER BY t.update_time DESC, t.id DESC</script>")
    List<GradePriceVO> selectCustomerPriceList(@Param("tenantId") Long tenantId,
                                               @Param("customerId") Long customerId,
                                               @Param("productId") Long productId,
                                               @Param("keyword") String keyword,
                                               @Param("brand") String brand);

    // ───────────────────────── 统一取价：规则行查询 ─────────────────────────

    /** 客户档案上的客户级别（价格规则按该级别匹配） */
    @Select("SELECT b.party_level FROM biz_party b WHERE b.id = CAST(#{customerId} AS BIGINT) AND b.deleted = 0")
    String selectCustomerLevel(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId);

    /** 命中优先级 1：客户指定价 */
    @Select("SELECT * FROM erp_customer_product_price WHERE deleted = 0"
        + " AND customer_id = CAST(#{customerId} AS BIGINT) AND product_id = CAST(#{productId} AS BIGINT)"
        + " AND (CAST(#{tenantId} AS BIGINT) IS NULL OR tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " ORDER BY update_time DESC, id DESC LIMIT 1")
    cn.aiedge.erp.stock.entity.CustomerProductPrice selectCustomerPriceRule(@Param("tenantId") Long tenantId,
                                                                            @Param("customerId") Long customerId,
                                                                            @Param("productId") Long productId);

    /** 命中优先级 2：客户级别指定价 */
    @Select("SELECT * FROM erp_customer_grade_price WHERE deleted = 0"
        + " AND grade_name = CAST(#{gradeName} AS VARCHAR) AND product_id = CAST(#{productId} AS BIGINT)"
        + " AND (CAST(#{tenantId} AS BIGINT) IS NULL OR tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " ORDER BY update_time DESC, id DESC LIMIT 1")
    cn.aiedge.erp.stock.entity.CustomerGradePrice selectGradePriceRule(@Param("tenantId") Long tenantId,
                                                                       @Param("gradeName") String gradeName,
                                                                       @Param("productId") Long productId);

    /** 命中优先级 3：客户级别折扣（级别默认价） */
    @Select("SELECT * FROM erp_customer_grade_discount WHERE deleted = 0"
        + " AND grade_name = CAST(#{gradeName} AS VARCHAR)"
        + " AND (CAST(#{tenantId} AS BIGINT) IS NULL OR tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " ORDER BY update_time DESC, id DESC LIMIT 1")
    cn.aiedge.erp.stock.entity.CustomerGradeDiscount selectGradeDiscountRule(@Param("tenantId") Long tenantId,
                                                                             @Param("gradeName") String gradeName);

    // ───────────────────────── 导入：按名称反查主键 ─────────────────────────

    /** 按商品名称精确匹配商品档案 */
    @Select("SELECT p.id FROM erp_product p WHERE p.deleted = 0 AND p.product_name = CAST(#{name} AS VARCHAR)"
        + " AND (CAST(#{tenantId} AS BIGINT) IS NULL OR p.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " ORDER BY p.id LIMIT 1")
    Long selectProductIdByName(@Param("tenantId") Long tenantId, @Param("name") String name);

    /** 按客户名称精确匹配往来单位（客户） */
    @Select("SELECT b.id FROM biz_party b WHERE b.deleted = 0 AND b.party_type = 1"
        + " AND b.party_name = CAST(#{name} AS VARCHAR)"
        + " AND (CAST(#{tenantId} AS BIGINT) IS NULL OR b.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " ORDER BY b.id LIMIT 1")
    Long selectCustomerIdByName(@Param("tenantId") Long tenantId, @Param("name") String name);
}
