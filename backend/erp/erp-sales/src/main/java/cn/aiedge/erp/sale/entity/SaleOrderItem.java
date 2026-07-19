package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售订单明细实体 - 生产级完整字段版
 * 对标 Odoo/SAP/金蝶/用友 等 ERP 系统销售订单明细规范
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_sale_order_item")
public class SaleOrderItem {

    /** 明细ID（主键） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 行号 */
    private Integer lineNo;

    // ═══ 商品信息 ═══
    /** 产品ID */
    private Long productId;
    /** 产品编码 */
    private String productCode;
    /** 产品名称 */
    private String productName;
    /** 图片 */
    private String image;
    /** 货号 */
    private String itemCode;
    /** 条码 */
    private String barcode;
    /** 小单位条码 */
    private String smallUnitBarcode;
    /** 规格 */
    private String specification;
    /** 型号 */
    private String model;
    /** 产地 */
    private String origin;
    /** 品牌 */
    private String brand;
    /** 保质期 */
    private String shelfLife;
    /** 单位 */
    private String unit;
    /** 计价单位 */
    private String pricingUnit;
    /** 小单位 */
    private String smallUnit;
    /** 商品行属性 */
    private String lineAttribute;
    /** 区域 */
    private String area;
    /** 货位 */
    private String location;

    // ═══ 批次信息 ═══
    /** 批次条码 */
    private String batchCode;
    /** 生产日期 */
    private LocalDate productionDate;
    /** 到期日期 */
    private LocalDate expiryDate;

    // ═══ 包装/数量 ═══
    /** 件散数量 */
    private BigDecimal quantity;
    /** 大包装 */
    private BigDecimal bigPack;
    /** 中包装 */
    private BigDecimal midPack;
    /** 小包装 */
    private BigDecimal smallPack;
    /** 小单位数量 */
    private BigDecimal smallUnitQuantity;

    // ═══ 库存相关 ═══
    /** 可用库存 */
    private BigDecimal availableStock;
    /** 可用库存换算结果 */
    private BigDecimal availableStockConverted;
    /** 账面库存 */
    private BigDecimal bookStock;
    /** 换算关系 */
    private String conversionRelation;
    /** 未发数量 */
    private BigDecimal unshippedQuantity;
    /** 已发数量 */
    private BigDecimal shippedQuantityDetail;

    // ═══ 价格信息 ═══
    /** 小单位单价 */
    private BigDecimal smallUnitPrice;
    /** 最近销售日期 */
    private LocalDate latestSaleDate;
    /** 最近售价 */
    private BigDecimal latestSalePrice;
    /** 零售价 */
    private BigDecimal retailPrice;
    /** 批发价 */
    private BigDecimal wholesalePrice;
    /** 最低售价 */
    private BigDecimal lowestPrice;
    /** 单价（不含税） */
    private BigDecimal unitPrice;
    /** 参考成本单价 */
    private BigDecimal costPrice;

    // ═══ 客户类型（价格等级标准化字段） ═══
    /** 餐饮店 */
    private Boolean restaurant;
    /** 食堂团餐 */
    private Boolean canteen;
    /** 自助vip */
    private Boolean vipSelf;
    /** 大团餐 */
    private Boolean largeGroup;
    /** 特价客户 */
    private Boolean specialCustomer;
    /** 外围餐饮店 */
    private Boolean outRestaurant;
    /** 重点|vip01 */
    private Boolean vipLevel1;
    /** 连锁|vip */
    private Boolean vipLevel2;

    // ═══ 预订货 ═══
    /** 预订货单编号 */
    private String preOrderNo;
    /** 使用预订货款 */
    private BigDecimal usePreOrderAmount;

    // ══ 折扣 ═══
    /** 折扣(%) */
    private BigDecimal discountRate;
    /** 优惠折扣(%) */
    private BigDecimal discountPercent;
    /** 折单原价 */
    private BigDecimal originalPrice;
    /** 折后单价 */
    private BigDecimal discountedUnitPrice;
    /** 惠后单价 */
    private BigDecimal favorableUnitPrice;

    // ═══ 积分/礼品 ═══
    /** 兑换礼品 */
    private String giftItem;
    /** 兑换积分 */
    private BigDecimal exchangePoints;
    /** 使用积分 */
    private BigDecimal usedPoints;

    // ═══ 物理属性 ═══
    /** 体积（m³） */
    private BigDecimal volume;
    /** 重量（kg） */
    private BigDecimal weight;

    // ═══ 赠品 ═══
    /** 赠品 */
    private Boolean gift;

    // ═══ 备注 ═══
    /** 备注 */
    private String remark;

    // ═══ 仓库 ═══
    /** 仓库ID */
    private Long warehouseId;

    // ═══ 价格等级（快照） ═══
    /** 客户等级代码 */
    private String customerGradeCode;
    /** 客户等级名称 */
    private String customerGradeName;
    /** 价格等级代码 */
    private String priceGradeCode;
    /** 价格来源 */
    private String priceSource;
    /** 计算得出的单价 */
    private BigDecimal calculatedPrice;
    /** 应用的折扣信息 */
    private String discountApplied;

    // ═══ 自定义字段 ══
    /** 单据自定义1(数字字段) */
    private BigDecimal customField1;
    /** 单据自定义2(数字字段) */
    private BigDecimal customField2;
    /** 单据自定义3(数字字段) */
    private BigDecimal customField3;
    /** 单据自定义4(文本字段) */
    private String customField4;
    /** 单据自定义5(文本字段) */
    private String customField5;
    /** 单据自定义6(数字字段) */
    private BigDecimal customField6;
    /** 单据自定义7(数字字段) */
    private BigDecimal customField7;
    /** 单据自定义8(往来单位ID) */
    private Long customField8;
    /** 单据自定义9(职员ID) */
    private Long customField9;
    /** 单据自定义10(部门ID) */
    private Long customField10;

    // ═══ 计算字段（不持久化） ═══
    /** 金额（不含税） */
    private BigDecimal amount;
    /** 参考成本金额 */
    private BigDecimal costAmount;
    /** 参考毛利 */
    private BigDecimal grossProfit;
    /** 折后金额 */
    private BigDecimal discountedAmount;
    /** 优惠后金额 */
    private BigDecimal favorableAmount;
    /** 税额 */
    private BigDecimal taxAmount;
    /** 含税单价 */
    private BigDecimal unitPriceWithTax;
    /** 金额（含税） */
    private BigDecimal amountWithTax;
    /** 折扣金额 */
    private BigDecimal discountAmount;
    /** 税率（%） */
    private BigDecimal taxRate;

    // ═══ 已出库数量 ═══
    /** 已出库数量 */
    private BigDecimal shippedQuantity;

    // ═══ 系统字段 ══
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
