package cn.aiedge.erp.purchase.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 采购单据 Excel 导入所需的「名称 → 主键」回查。
 *
 * <p>导入模板里只有人可读的名称（供应商名 / 仓库名 / 商品名），而单据明细必须落**主键**，
 * 否则单据虽有行但关联不到任何主数据，后续收货、库存、统计全链路拿不到有效数据。</p>
 *
 * <p>三张表（erp_product / erp_warehouse / biz_party）都带 tenant_id 且不在忽略清单内，
 * 租户条件由多租户拦截器注入 —— 即**只在当前租户的主数据里解析**，解析不到就报错，
 * 绝不跨租户猜。</p>
 */
@Mapper
public interface PurchaseImportLookupMapper {

    /** 商品：按名称或编码（含别名）定位，取第一条。 */
    @Select("SELECT id FROM erp_product"
            + " WHERE deleted = 0"
            + "   AND (product_name = #{name} OR product_code = #{name} OR product_code_alias = #{name})"
            + " ORDER BY id LIMIT 1")
    Long selectProductIdByName(@Param("name") String name);

    @Select("SELECT product_code FROM erp_product WHERE id = #{productId} AND deleted = 0")
    String selectProductCode(@Param("productId") Long productId);

    @Select("SELECT spec FROM erp_product WHERE id = #{productId} AND deleted = 0")
    String selectProductSpec(@Param("productId") Long productId);

    @Select("SELECT unit FROM erp_product WHERE id = #{productId} AND deleted = 0")
    String selectProductUnit(@Param("productId") Long productId);

    /** 仓库：按名称定位。 */
    @Select("SELECT id FROM erp_warehouse WHERE deleted = 0 AND warehouse_name = #{name} ORDER BY id LIMIT 1")
    Long selectWarehouseIdByName(@Param("name") String name);

    /** 往来单位（供应商）：按名称定位。 */
    @Select("SELECT id FROM biz_party WHERE deleted = 0 AND party_name = #{name} ORDER BY id LIMIT 1")
    Long selectPartyIdByName(@Param("name") String name);

    @Select("SELECT party_code FROM biz_party WHERE id = #{partyId} AND deleted = 0")
    String selectPartyCode(@Param("partyId") Long partyId);
}
