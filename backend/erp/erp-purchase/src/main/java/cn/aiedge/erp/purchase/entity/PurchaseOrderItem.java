package cn.aiedge.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购订单明细实体 - 生产级完整字段版
 * 对标 SaleOrderItem 结构，去掉 8 个销售专属价格等级字段
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_purchase_order_item")
public class PurchaseOrderItem {

    /** 明细ID（主键） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 行号 */
    private Integer lineNo;

    // ═══ 商品信息（快照） ═══
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
    /** 计价单位 */
    private String unit;
    /** 计价单位(冗余) */
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
    /** 换算关系 */
    private String conversionRelation;

    // ═══ 库存快照 ═══
    /** 可用库存 */
    private BigDecimal availableStock;
    /** 可用库存换算 */
    private BigDecimal availableStockConverted;
    /** 账面库存 */
    private BigDecimal bookStock;
    /** 未收数量 */
    private BigDecimal unshippedQuantity;
    /** 已收数量(明细) */
    private BigDecimal receivedQuantityDetail;
    /** 终止数量 */
    private BigDecimal terminatedQuantity;
    /** 终止金额 */
    private BigDecimal terminatedAmount;

    // ═══ 价格信息 ═══
    /** 小单位单价 */
    private BigDecimal smallUnitPrice;
    /** 最近采购日期 */
    private LocalDate latestPurchaseDate;
    /** 最近采购价 */
    private BigDecimal latestPurchasePrice;
    /** 零售价(参考) */
    private BigDecimal retailPrice;
    /** 批发价(参考) */
    private BigDecimal wholesalePrice;
    /** 单价(不含税) */
    private BigDecimal unitPrice;
    /** 成本单价 */
    private BigDecimal costPrice;
    /** 成本金额 */
    private BigDecimal costAmount;

    // ═══ 折扣 ═══
    /** 优惠折扣(%) */
    private BigDecimal discountRate;
    /** 惠后单价 */
    private BigDecimal discountedUnitPrice;
    /** 惠后金额 */
    private BigDecimal discountedAmount;
    /** 折单原价 */
    private BigDecimal originalPrice;

    // ═══ 物理属性 ═══
    /** 体积(m³) */
    private BigDecimal volume;
    /** 重量(kg) */
    private BigDecimal weight;

    // ═══ 赠品 ═══
    /** 赠品 */
    private Boolean gift;

    // ═══ 备注 ═══
    /** 明细备注 */
    private String remark;

    // ═══ 仓库 ═══
    /** 仓库ID */
    private Long warehouseId;

    // ═══ 自定义字段 1~10（DB 列名带下划线数字） ═══
    /** 单据自定义1(数字) */
    @TableField("custom_field_1")
    private BigDecimal customField1;
    /** 单据自定义2(数字) */
    @TableField("custom_field_2")
    private BigDecimal customField2;
    /** 单据自定义3(数字) */
    @TableField("custom_field_3")
    private BigDecimal customField3;
    /** 单据自定义4(文本) */
    @TableField("custom_field_4")
    private String customField4;
    /** 单据自定义5(文本) */
    @TableField("custom_field_5")
    private String customField5;
    /** 单据自定义6(数字) */
    @TableField("custom_field_6")
    private BigDecimal customField6;
    /** 单据自定义7(数字) */
    @TableField("custom_field_7")
    private BigDecimal customField7;
    /** 单据自定义8(往来单位ID) */
    @TableField("custom_field_8")
    private Long customField8;
    /** 单据自定义9(职员ID) */
    @TableField("custom_field_9")
    private Long customField9;
    /** 单据自定义10(部门ID) */
    @TableField("custom_field_10")
    private Long customField10;

    // ═══ 计算字段 ═══
    /** 金额(不含税) */
    private BigDecimal amount;
    /** 税率(%) */
    private BigDecimal taxRate;
    /** 税额 */
    private BigDecimal taxAmount;
    /** 含税单价 */
    private BigDecimal unitPriceWithTax;
    /** 金额(含税) */
    private BigDecimal amountWithTax;
    /** 折扣金额 */
    private BigDecimal discountAmount;
    /** 已收货数量(冗余) */
    private BigDecimal receivedQuantity;

    // ═══ 系统字段 ═══
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
