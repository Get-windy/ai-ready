package cn.aiedge.erp.purchase.replenishment;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 「补货建议 → 采购订单」所需的只读回查。
 *
 * <p>补货建议表 {@code erp_stock_replenishment} 只存了商品编码与名称，建采购订单需要
 * 商品主键与采购价，故按编码回查商品主数据。三张表（erp_product / biz_party）
 * 均带 tenant_id 且不在忽略清单内，租户条件由多租户拦截器注入。</p>
 */
@Mapper
public interface ReplenishmentProductMapper {

    /**
     * 按商品编码（含别名）查商品主键；查不到返回 null。
     */
    @Select("SELECT id FROM erp_product"
            + " WHERE deleted = 0"
            + "   AND (product_code = #{productCode} OR product_code_alias = #{productCode})"
            + " ORDER BY id LIMIT 1")
    Long selectProductIdByCode(@Param("productCode") String productCode);

    /**
     * 商品采购价（约定价），缺失返回 null 由调用方兜底为 0。
     */
    @Select("SELECT purchase_price FROM erp_product WHERE id = #{productId} AND deleted = 0")
    BigDecimal selectPurchasePrice(@Param("productId") Long productId);

    /**
     * 往来单位名称（供应商快照用）。
     */
    @Select("SELECT party_name FROM biz_party WHERE id = #{partyId} AND deleted = 0")
    String selectPartyName(@Param("partyId") Long partyId);

    /**
     * 往来单位编号（供应商快照用）。
     */
    @Select("SELECT party_code FROM biz_party WHERE id = #{partyId} AND deleted = 0")
    String selectPartyCode(@Param("partyId") Long partyId);
}
