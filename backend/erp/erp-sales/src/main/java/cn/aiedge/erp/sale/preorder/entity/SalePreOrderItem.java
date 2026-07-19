package cn.aiedge.erp.sale.preorder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预订货单明细实体
 */
@Data
@TableName("erp_sale_pre_order_item")
public class SalePreOrderItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 关联预订货单ID */
    private Long orderId;
    /** 行号 */
    private Integer lineNo;

    // ─── 商品信息 ───
    /** 商品ID */
    private Long productId;
    /** 商品图片 */
    private String imageUrl;
    /** 商品名称 */
    private String productName;
    /** 货号 */
    private String productCode;
    /** 条码 */
    private String barcode;
    /** 规格 */
    private String specification;
    /** 型号 */
    private String model;
    /** 产地 */
    private String origin;
    /** 品牌 */
    private String brand;

    // ─── 单位与换算 ───
    /** 单位 */
    private String unit;
    /** 计价单位 */
    private String pricingUnit;
    /** 小单位 */
    private String smallUnit;
    /** 小单位数量 */
    private BigDecimal smallUnitQuantity;
    /** 换算关系 */
    private String conversionRelation;
    /** 换算结果 */
    private BigDecimal conversionResult;

    // ─── 库存 ───
    /** 区域 */
    private String region;
    /** 货位 */
    private String location;
    /** 可用库存 */
    private BigDecimal availableStock;
    /** 可用库存换算结果 */
    private BigDecimal availableStockConversion;
    /** 账面库存 */
    private BigDecimal bookStock;

    // ─── 数量 ───
    /** 预订数量 */
    private BigDecimal quantity;
    /** 件散数量 */
    private BigDecimal pieceQuantity;
    /** 大包装 */
    private BigDecimal bigPack;
    /** 中包装 */
    private BigDecimal midPack;
    /** 小包装 */
    private BigDecimal smallPack;
    /** 已订数量 */
    private BigDecimal orderedQuantity;
    /** 未订数量 */
    private BigDecimal unOrderedQuantity;
    /** 已发数量 */
    private BigDecimal shippedQuantity;
    /** 未发数量 */
    private BigDecimal unShippedQuantity;
    /** 终止数量 */
    private BigDecimal terminateQuantity;
    /** 终止金额 */
    private BigDecimal terminateAmount;

    // ─── 价格 ───
    /** 最近销售日期 */
    private LocalDate lastSaleDate;
    /** 零售价 */
    private BigDecimal retailPrice;
    /** 批发价 */
    private BigDecimal wholesalePrice;
    /** 最低售价 */
    private BigDecimal minSalePrice;
    /** 单价 */
    private BigDecimal unitPrice;
    /** 金额 */
    private BigDecimal amount;
    /** 小单位单价 */
    private BigDecimal smallUnitPrice;

    // ─── 折扣 ───
    /** 折扣(%) */
    private BigDecimal discountRate;
    /** 折后单价 */
    private BigDecimal discountedPrice;
    /** 折后金额 */
    private BigDecimal discountedAmount;

    // ─── 成本与毛利 ───
    /** 参考成本单价 */
    private BigDecimal costPrice;
    /** 参考成本金额 */
    private BigDecimal costAmount;
    /** 参考毛利 */
    private BigDecimal grossProfit;

    // ─── 体积重量 ───
    /** 体积(m³) */
    private BigDecimal volume;
    /** 重量(kg) */
    private BigDecimal weight;

    // ─── 属性 ───
    /** 商品行属性 */
    private String productAttribute;
    /** 是否赠品 */
    private Boolean gift;
    /** 明细备注 */
    private String remark;

    // ─── 价格等级(8个) ───
    /** 价格等级1 */
    private BigDecimal priceLevel1;
    /** 价格等级2 */
    private BigDecimal priceLevel2;
    /** 价格等级3 */
    private BigDecimal priceLevel3;
    /** 价格等级4 */
    private BigDecimal priceLevel4;
    /** 价格等级5 */
    private BigDecimal priceLevel5;
    /** 价格等级6 */
    private BigDecimal priceLevel6;
    /** 价格等级7 */
    private BigDecimal priceLevel7;
    /** 价格等级8 */
    private BigDecimal priceLevel8;

    // ─── 表体自定义字段 ───
    /** 表体自定义1(数字) */
    private BigDecimal extNum1;
    /** 表体自定义2(数字) */
    private BigDecimal extNum2;
    /** 表体自定义3(数字) */
    private BigDecimal extNum3;
    /** 表体自定义4(文本) */
    private String extText1;
    /** 表体自定义5(文本) */
    private String extText2;
    /** 表体自定义6(数字) */
    private BigDecimal extNum4;
    /** 表体自定义7(数字) */
    private BigDecimal extNum5;
    /** 表体自定义8(往来单位) */
    private Long extPartner;
    /** 表体自定义9(职员) */
    private Long extStaff;
    /** 表体自定义10(部门) */
    private Long extDept;

    // 注意：不再冗余主表字段（如customer_name, order_date, warehouse_name等）
    // 这些字段通过JOIN主表获取，符合数据库第三范式

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
