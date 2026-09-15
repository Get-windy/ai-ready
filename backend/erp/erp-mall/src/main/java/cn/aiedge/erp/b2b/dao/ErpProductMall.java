package cn.aiedge.erp.b2b.dao;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * erp_product 表映射（商城模块只读视图）
 * 替代 mall_product 表，消除数据冗余
 */
@Data
@TableName("v_mall_product")
public class ErpProductMall implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 关联 erp_product.product_code */
    private String productId;

    private String productCode;

    private String productName;

    private String imageUrl;

    /** 零售价 → 商城销售价 */
    private BigDecimal salePrice;

    /** 批发价 → 商城市场价 */
    private BigDecimal marketPrice;

    /** 可用库存（来自 erp_stock 实时计算） */
    private Integer stockQuantity;

    private String categoryId;

    private String categoryName;

    /** ON_SHELF / INACTIVE */
    private String status;

    private String description;

    /** 商城累计销量 */
    private Integer salesCount;

    // ── V11.361.5：视图追加列（erp_product.spec / erp_product.unit） ──
    /** 规格（erp_product.spec → 视图 specification） */
    private String specification;

    /** 单位（erp_product.unit → 视图 unit_name） */
    private String unitName;

    // ── V11.361.6：v_mall_product 追加「商品上架」金标准列（Flyway V11.361.6） ──
    /** 条形码（erp_product.barcode → 视图 barcode） */
    private String barcode;

    /** 型号（erp_product.model → 视图 model_no） */
    private String modelNo;

    /** 产地（erp_product.origin → 视图 origin_place） */
    private String originPlace;

    /** 品牌（erp_product.brand → 视图 brand） */
    private String brand;

    /** 批发价（erp_product.wholesale_price → 视图 wholesale_price；与 marketPrice 同源，
     *  marketPrice 为商城历史「市场价」口径，保留不动） */
    private BigDecimal wholesalePrice;

    /** 预设进价（COALESCE(erp_product.purchase_price, cost_price, 0) → 视图 preset_cost_price） */
    private BigDecimal presetCostPrice;

    /** 商城排序方式（erp_product.mall_sort_type → 视图 sort：DEFAULT/SALES/MANUAL） */
    private String sort;

    /** 商城排序值（erp_product.mall_sort_order → 视图 sort_value） */
    private Integer sortValue;

    /** 商城起订量（erp_product.mall_min_order_qty → 视图 min_order_quantity） */
    private Integer minOrderQuantity;

    /** 商品积分（erp_product.mall_points → 视图 product_points） */
    private BigDecimal productPoints;

    /** 备注（erp_product.remark → 视图 remark） */
    private String remark;

    /** 商城检索关键字（erp_product.keywords → 视图 keyword） */
    private String keyword;

    /** 使用优惠券原始值（erp_product.use_coupon → 视图 use_coupon：0=否 1=是） */
    private Integer useCoupon;

    /** 使用优惠券布尔值（erp_product.use_coupon = 1 → 视图 coupon_used） */
    private Boolean couponUsed;

    /** 商品类型（erp_product.product_type → 视图 product_type：SINGLE/KIT/SERVICE） */
    private String productType;

    /** 显示状态（erp_product.status → 视图 visible_status：ENABLED/DISABLED），非商城上架状态 */
    private String visibleStatus;

    /** 商品标签（erp_product.mall_tags → 视图 product_tag，逗号分隔的标准槽位编码 TAG_1..TAG_20） */
    private String productTag;

    /** 所属行业类别（erp_product.industry_category → 视图 industry_category） */
    private String industryCategory;

    // ── 客户类型价格 8 列（对标列 15-22；视图 grade_price_1..8 ← MAX(erp_product_unit.grade_price_N)） ──
    // ⚠️ 必须显式写 @TableField：MyBatis-Plus 的 camelToUnderline 对「驼峰+数字结尾」生成
    //    `grade_price1`（数字前**不**补下划线），而视图列名是 `grade_price_1` → 不加注解会
    //    抛 BadSqlGrammarException「字段 grade_price1 不存在」，导致 /product/page 整个 500。
    /** 价格等级1 餐饮店（该商品多单位取最大值；>0 视为已配价） */
    @TableField("grade_price_1")
    private BigDecimal gradePrice1;

    /** 价格等级2 食堂团餐 */
    @TableField("grade_price_2")
    private BigDecimal gradePrice2;

    /** 价格等级3 外围餐饮店 */
    @TableField("grade_price_3")
    private BigDecimal gradePrice3;

    /** 价格等级4 自助vip */
    @TableField("grade_price_4")
    private BigDecimal gradePrice4;

    /** 价格等级5 大团餐 */
    @TableField("grade_price_5")
    private BigDecimal gradePrice5;

    /** 价格等级6 重点|vip01 */
    @TableField("grade_price_6")
    private BigDecimal gradePrice6;

    /** 价格等级7 连锁|vip */
    @TableField("grade_price_7")
    private BigDecimal gradePrice7;

    /** 价格等级8 特价客户 */
    @TableField("grade_price_8")
    private BigDecimal gradePrice8;

    /** 已配置级别指定价的客户等级昵称集合（erp_customer_grade_price.grade_name 聚合，
     *  逗号分隔升序；昵称为用户自定义，作为 8 个 gradePriceN 的补充佐证） */
    private String customerGradeCodes;

    @TableLogic
    private Integer deleted;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
