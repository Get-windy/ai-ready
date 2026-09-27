package cn.aiedge.erp.b2b.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductListDTO {
    private Long id;
    private String productId;
    private String productName;
    private String imageUrl;
    private BigDecimal salePrice;
    private BigDecimal marketPrice;
    private Integer stockQuantity;
    private Integer salesCount;

    // ── 2026-09-26 新增：C 端「商品卡」要用的字段 ──
    // 背景：此前 DTO 只有 8 个字段，前端《商城App设计方案》§4.1 要求的
    // 标签角标 / 规格 / 卖点条 / 起订量 / 分类角标 **一个都拿不到** ——
    // 页面写好了也是空的。字段来源均为 v_mall_product 视图已有列，零额外查询。

    /** 分类（左侧角标，如「预制菜」优先用 industryCategory） */
    private String categoryId;
    private String categoryName;
    /** 行业分类（对标橱窗的行业角标） */
    private String industryCategory;
    /** 规格，如「3成熟 净重10kg/箱」 */
    private String specification;
    /** 单位名（配合「拆零单位」开关展示） */
    private String unitName;
    /** 商品标签：逗号分隔的槽位码（TAG_1,TAG_5），前端翻译成标签名做角标 */
    private String productTag;
    /** 卖点/描述首行（对标截图里的红底卖点条） */
    private String description;
    /** 起订量 */
    private Integer minOrderQuantity;
    /** 商品类型 SINGLE/KIT/SERVICE */
    private String productType;
}
