package cn.aiedge.erp.purchase.purchasereturn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_purchase_return_item")
public class PurchaseReturnItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long returnId;

    private Integer lineNo;

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    /** 图片 */
    private String image;

    /** 条码 */
    private String barcode;

    /** 型号 */
    private String model;

    /** 产地 */
    private String origin;

    /** 品牌 */
    private String brand;

    /** 区域 */
    private String region;

    /** 货位 */
    private String location;

    // ── 库存 ──
    /** 可用库存 */
    private BigDecimal availableStock;

    /** 可用库存换算结果 */
    private BigDecimal availableStockConverted;

    /** 账面库存 */
    private BigDecimal bookStock;

    // ── 批次 ──
    /** 批次号 */
    private String batchNo;

    /** 批次条码 */
    private String batchCode;

    /** 生产日期 */
    private LocalDate productionDate;

    /** 保质期 */
    private String shelfLife;

    /** 到期日期 */
    private LocalDate expiryDate;

    /** 退货数量 */
    private BigDecimal returnQuantity;

    /** 换算关系 */
    private String conversionRelation;

    /** 件散数量 */
    private BigDecimal pieceQuantity;

    /** 大包装 */
    private BigDecimal bigPack;

    /** 中包装 */
    private BigDecimal midPack;

    /** 小包装 */
    private BigDecimal smallPack;

    // ── 价格 ──
    /** 最近采购日期 */
    private LocalDate latestPurchaseDate;

    /** 零售价 */
    private BigDecimal retailPrice;

    /** 批发价 */
    private BigDecimal wholesalePrice;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 金额 */
    private BigDecimal lineAmount;

    /** 小单位 */
    private String smallUnit;

    /** 小单位单价 */
    private BigDecimal smallUnitPrice;

    /** 小单位数量 */
    private BigDecimal smallUnitQuantity;

    /** 成本单价 */
    private BigDecimal unitCost;

    /** 成本金额 */
    private BigDecimal costAmount;

    /** 税率(%) */
    private BigDecimal taxRate;

    /** 税额 */
    private BigDecimal taxAmount;

    /** 价税合计 */
    private BigDecimal lineTotal;

    /** 体积(m³) */
    private BigDecimal volume;

    /** 重量(kg) */
    private BigDecimal weight;

    /** 赠品 */
    private Boolean gift;

    // ── 价格等级（标准化产品价格等级） ──
    private Boolean restaurant;
    private Boolean canteen;
    private Boolean outRestaurant;
    private Boolean vipSelf;
    private Boolean largeGroup;
    private Boolean vipLevel1;
    private Boolean vipLevel2;
    private Boolean specialCustomer;

    // ── 单据自定义字段 ──
    private BigDecimal customField1;
    private BigDecimal customField2;
    private BigDecimal customField3;
    private String customField4;
    private String customField5;
    private BigDecimal customField6;
    private BigDecimal customField7;
    private String customField8;
    private String customField9;
    private String customField10;

    /** 退货原因 */
    private String reason;

    private String remark;

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
