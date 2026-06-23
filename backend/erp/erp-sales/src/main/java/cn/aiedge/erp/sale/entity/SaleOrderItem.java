package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售订单明细实体 - 生产级分布式存储重构版
 * 遵循快照存储、关联JOIN、实时计算的数据分布策略
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_sale_order_item")
public class SaleOrderItem {

    /**
     * 明细ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 行号
     */
    private Integer lineNo;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 产品编码
     */
    private String productCode;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 预订货单编号 - 快照存储
     */
    private String preOrderNo;

    /**
     * 规格
     */
    private String specification;

    /**
     * 型号 - 快照存储
     */
    private String model;

    /**
     * 货位
     */
    private String location;

    /**
     * 批次条码
     */
    private String batchCode;

    /**
     * 生产日期
     */
    private LocalDate productionDate;

    /**
     * 到期日期
     */
    private LocalDate expiryDate;

    /**
     * 订购数量
     */
    private BigDecimal quantity;

    /**
     * 单价（不含税）
     */
    private BigDecimal unitPrice;

    /**
     * 金额（不含税）
     */
    private BigDecimal amount;

    /**
     * 参考成本单价
     */
    private BigDecimal costPrice;

    /**
     * 折单原价
     */
    private BigDecimal originalPrice;

    /**
     * 税率（%）
     */
    private BigDecimal taxRate;

    /**
     * 含税单价
     */
    private BigDecimal unitPriceWithTax;

    /**
     * 金额（含税）
     */
    private BigDecimal amountWithTax;

    /**
     * 折扣率（%）
     */
    private BigDecimal discountRate;

    /**
     * 优惠折扣(%)
     */
    private BigDecimal discountPercent;

    /**
     * 折扣金额
     */
    private BigDecimal discountAmount;

    /**
     * 使用预订货款
     */
    private BigDecimal usePreOrderAmount;

    /**
     * 大包装数量
     */
    private BigDecimal bigPack;

    /**
     * 中包装数量
     */
    private BigDecimal midPack;

    /**
     * 小包装数量
     */
    private BigDecimal smallPack;

    /**
     * 区域 - 快照存储
     */
    private String area;

    /**
     * 小单位
     */
    private String smallUnit;

    /**
     * 小单位单价
     */
    private BigDecimal smallUnitPrice;

    /**
     * 小单位数量
     */
    private BigDecimal smallUnitQuantity;

    /**
     * 赠品
     */
    private Boolean gift;

    /**
     * 兑换礼品
     */
    private String giftItem;

    /**
     * 兑换积分
     */
    private BigDecimal exchangePoints;

    /**
     * 使用积分
     */
    private BigDecimal usedPoints;

    /**
     * 备注
     */
    private String remark;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    // ===== 新增价格等级字段 =====

    /**
     * 客户等级代码 - 快照存储
     */
    private String customerGradeCode;

    /**
     * 客户等级名称 - 快照存储
     */
    private String customerGradeName;

    /**
     * 价格等级代码 - 快照存储
     */
    private String priceGradeCode;

    /**
     * 价格来源 - 快照存储
     */
    private String priceSource;

    /**
     * 计算得出的单价 - 快照存储
     */
    private BigDecimal calculatedPrice;

    /**
     * 应用的折扣信息 - 快照存储
     */
    private String discountApplied;

    // ===== 自定义字段 =====
    /**
     * 单据自定义1(数字字段)
     */
    private BigDecimal customField1;

    /**
     * 单据自定义2(数字字段)
     */
    private BigDecimal customField2;

    /**
     * 单据自定义3(数字字段)
     */
    private BigDecimal customField3;

    /**
     * 单据自定义4(文本字段)
     */
    private String customField4;

    /**
     * 单据自定义5(文本字段)
     */
    private String customField5;

    /**
     * 单据自定义6(数字字段)
     */
    private BigDecimal customField6;

    /**
     * 单据自定义7(数字字段)
     */
    private BigDecimal customField7;

    /**
     * 单据自定义8(往来单位ID)
     */
    private Long customField8;

    /**
     * 单据自定义9(职员ID)
     */
    private Long customField9;

    /**
     * 单据自定义10(部门ID)
     */
    private Long customField10;

    // ===== 标记为JOIN获取的字段（不持久化到数据库）=====

    /**
     * 图片 - 通过产品ID关联获取
     */
    @TableField(exist = false)
    private String image;

    /**
     * 条码 - 通过产品ID关联获取
     */
    @TableField(exist = false)
    private String barcode;

    /**
     * 小单位条码 - 通过产品ID关联获取
     */
    @TableField(exist = false)
    private String smallUnitBarcode;

    /**
     * 产地 - 通过产品ID关联获取
     */
    @TableField(exist = false)
    private String origin;

    /**
     * 品牌 - 通过产品ID关联获取
     */
    @TableField(exist = false)
    private String brand;

    /**
     * 保质期 - 通过产品ID关联获取
     */
    @TableField(exist = false)
    private String shelfLife;

    /**
     * 零售价 - 通过产品ID关联获取
     */
    @TableField(exist = false)
    private BigDecimal retailPrice;

    /**
     * 批发价 - 通过产品ID关联获取
     */
    @TableField(exist = false)
    private BigDecimal wholesalePrice;

    /**
     * 单位 - 通过产品ID关联获取
     */
    @TableField(exist = false)
    private String unit;

    /**
     * 商品行属性 - 通过产品ID关联获取
     */
    @TableField(exist = false)
    private String lineAttribute;

    /**
     * 体积（m³） - 通过产品ID关联获取
     */
    @TableField(exist = false)
    private BigDecimal volume;

    /**
     * 重量（kg） - 通过产品ID关联获取
     */
    @TableField(exist = false)
    private BigDecimal weight;

    // ===== 标记为实时计算的字段（不持久化到数据库）=====

    /**
     * 可用库存 - 通过库存服务API实时获取
     */
    @TableField(exist = false)
    private BigDecimal availableStock;

    /**
     * 可用库存换算结果 - 通过库存服务+换算实时获取
     */
    @TableField(exist = false)
    private BigDecimal availableStockConverted;

    /**
     * 账面库存 - 通过库存服务API实时获取
     */
    @TableField(exist = false)
    private BigDecimal bookStock;

    /**
     * 换算关系 - 通过产品+单位换算实时获取
     */
    @TableField(exist = false)
    private String conversionRelation;

    /**
     * 最近销售日期 - 通过价格记忆表关联获取
     */
    @TableField(exist = false)
    private LocalDateTime latestSaleDate;

    /**
     * 最近售价 - 通过价格记忆表关联获取
     */
    @TableField(exist = false)
    private BigDecimal latestSalePrice;

    /**
     * 最低售价 - 通过价格引擎实时获取
     */
    @TableField(exist = false)
    private BigDecimal lowestPrice;

    /**
     * 未发数量 - 通过发货单汇总计算
     */
    @TableField(exist = false)
    private BigDecimal unshippedQuantity;

    /**
     * 已发数量 - 通过发货单汇总计算
     */
    @TableField(exist = false)
    private BigDecimal shippedQuantityDetail;

    /**
     * 参考成本金额 - 通过计算得出 (quantity * costPrice)
     */
    @TableField(exist = false)
    private BigDecimal costAmount;

    /**
     * 参考毛利 - 通过计算得出 (amount - costAmount)
     */
    @TableField(exist = false)
    private BigDecimal grossProfit;

    /**
     * 折后单价 - 通过计算得出 (unitPrice * (1 - discountRate/100))
     */
    @TableField(exist = false)
    private BigDecimal discountedUnitPrice;

    /**
     * 折后金额 - 通过计算得出 (amount * (1 - discountRate/100))
     */
    @TableField(exist = false)
    private BigDecimal discountedAmount;

    /**
     * 惠后单价 - 通过计算得出 (discountedUnitPrice * (1 - discountPercent/100))
     */
    @TableField(exist = false)
    private BigDecimal favorableUnitPrice;

    /**
     * 优惠后金额 - 通过计算得出 (discountedAmount * (1 - discountPercent/100))
     */
    @TableField(exist = false)
    private BigDecimal favorableAmount;

    /**
     * 税额 - 通过计算得出 (amount * taxRate / 100)
     */
    @TableField(exist = false)
    private BigDecimal taxAmount;

    /**
     * 已出库数量 - 订单创建时为0，发货时累加
     */
    private BigDecimal shippedQuantity;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}