package cn.aiedge.erp.purchase.purchaseexchange.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购换货单明细
 *
 * <p>字段按业务实体分组，支持换入仓库和换出仓库两种类型（通过warehouseType区分）</p>
 * <ul>
 *   <li>商品信息快照：productId, productName, productCode, barcode, specification, model, origin, brand</li>
 *   <li>库存信息：availableStock, stockConverted, bookStock</li>
 *   <li>数量和包装：quantity, conversionRate, pieceScatterQty, largePackage, mediumPackage, smallPackage</li>
 *   <li>价格信息：unitPrice, amount, retailPrice, wholesalePrice等</li>
 *   <li>价格等级（8个）：priceLevel1-8</li>
 *   <li>自定义字段：extNum1-10, extText1-6</li>
 * </ul>
 */
@Data
@TableName("erp_purchase_exchange_item")
public class PurchaseExchangeItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long exchangeId;

    // ========== 仓库类型标记 ==========
    /**
     * 仓库类型: 1=换入, 2=换出
     */
    private Integer warehouseType;

    // ========== 商品信息快照 ==========
    private Long productId;
    private String productName;
    private String productCode;
    private String barcode;
    private String specification;
    private String model;
    private String origin;
    private String brand;
    private String unit;
    private String image;
    private String location;
    private String area;

    // ========== 库存信息 ==========
    private BigDecimal availableStock;
    private BigDecimal stockConverted;
    private BigDecimal bookStock;

    // ========== 批次和日期 ==========
    private String batchBarcode;
    private LocalDate productionDate;
    private Integer shelfLife;
    private LocalDate expiryDate;

    // ========== 数量和包装 ==========
    private BigDecimal quantity;
    private BigDecimal conversionRate;
    private BigDecimal pieceScatterQty;
    private BigDecimal largePackage;
    private BigDecimal mediumPackage;
    private BigDecimal smallPackage;

    // ========== 价格信息 ==========
    private LocalDate recentPurchaseDate;
    private BigDecimal retailPrice;
    private BigDecimal wholesalePrice;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String smallUnit;
    private BigDecimal smallUnitPrice;
    private BigDecimal smallUnitQty;
    private BigDecimal costPrice;
    private BigDecimal costAmount;

    // ========== 折扣信息 ==========
    private BigDecimal discount;
    private BigDecimal discountPrice;
    private BigDecimal discountAmount;

    // ========== 物理属性 ==========
    private BigDecimal volume;
    private BigDecimal weight;

    // ========== 业务标记 ==========
    private Boolean isGift;
    private String remark;

    // ========== 价格等级（8个） ==========
    // ⚠️ 必须显式写 @TableField：DB 列是 price_level_1（V11.47.0 建），
    // 而 MyBatis-Plus 的驼峰转下划线对 priceLevel1 会推导成 price_level1（数字前不补下划线）
    // → 缺注解时全列查询拼出 price_level1，PostgreSQL 报「字段不存在」→ 换货单明细整块 500。
    // 同类字段的修正先例见 PurchaseOrderSettlement.depositAccount1、PurchaseOrderItem.customField1。
    @TableField("price_level_1")
    private BigDecimal priceLevel1;
    @TableField("price_level_2")
    private BigDecimal priceLevel2;
    @TableField("price_level_3")
    private BigDecimal priceLevel3;
    @TableField("price_level_4")
    private BigDecimal priceLevel4;
    @TableField("price_level_5")
    private BigDecimal priceLevel5;
    @TableField("price_level_6")
    private BigDecimal priceLevel6;
    @TableField("price_level_7")
    private BigDecimal priceLevel7;
    @TableField("price_level_8")
    private BigDecimal priceLevel8;

    // ========== 自定义字段（数字） ==========
    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private BigDecimal extNum3;
    private BigDecimal extNum4;
    private BigDecimal extNum5;
    private BigDecimal extNum6;
    private BigDecimal extNum7;
    private BigDecimal extNum8;
    private BigDecimal extNum9;
    private BigDecimal extNum10;

    // ========== 自定义字段（文本） ==========
    private String extText1;
    private String extText2;
    private String extText3;
    private String extText4;
    private String extText5;
    private String extText6;

    // ========== 关联字段 ==========
    private Long extPartner;
    private Long extStaff;
    private Long extDept;

    // ========== 标准审计字段 ==========
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
