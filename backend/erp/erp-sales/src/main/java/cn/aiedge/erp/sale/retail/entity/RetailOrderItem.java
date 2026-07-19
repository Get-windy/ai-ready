package cn.aiedge.erp.sale.retail.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 零售单明细行实体
 * 遵循主从表快照模型：存外键ID + 快照字段（成交后锁定不变）
 * 预留 ext_num/ext_text/ext_partner/ext_staff/ext_dept 自定义扩展字段
 */
@Data
@Accessors(chain = true)
@TableName("erp_retail_order_item")
public class RetailOrderItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 所属零售单ID */
    private Long orderId;

    /** 行序号 */
    private Integer lineNo;

    // ═══ 产品快照 ═══
    private Long productId;
    private String productCode;        // 货号
    private String productName;        // 商品名称
    private String imageUrl;           // 图片URL
    private String barcode;            // 条码
    private String smallUnitBarcode;   // 小单位条码
    private String specification;      // 规格
    private String model;              // 型号
    private String origin;             // 产地
    private String brand;              // 品牌
    private String productAttribute;   // 商品行属性
    private String unit;               // 计价单位

    // ═══ 货位/区域 ═══
    private String location;           // 货位
    private String region;             // 区域
    private Integer warehouseLocationId;
    private String warehouseLocationCode;

    // ═══ 数量 ═══
    private BigDecimal quantity;               // 销售数量
    private BigDecimal outboundQuantity;       // 出库数量
    private BigDecimal pendingQuantity;        // 待处理数量
    private BigDecimal pieceQuantity;          // 件散数量
    private BigDecimal receivedQuantity;       // 已收数量

    // ═══ 包装 ═══
    private Integer bigPack;                   // 大包装
    private Integer midPack;                   // 中包装
    private Integer smallPack;                 // 小包装

    // ═══ 单位换算 ═══
    private String conversionRelation;         // 换算关系
    private BigDecimal conversionResult;       // 换算结果
    private String smallUnit;                  // 小单位
    private BigDecimal smallUnitQuantity;      // 小单位数量

    // ═══ 价格快照 ═══
    private BigDecimal unitPrice;              // 单价
    private BigDecimal lineAmount;             // 行金额
    private BigDecimal amount;                 // 金额（兼容字段）
    private BigDecimal originalPrice;          // 折单原价
    private BigDecimal smallUnitPrice;         // 小单位单价
    private BigDecimal taxRate;                // 税率

    // ═══ 折扣 ═══
    private BigDecimal discountRate;           // 折扣(%)
    private BigDecimal discountedAmount;       // 折后金额
    private BigDecimal discountedPrice;        // 折后单价
    private BigDecimal favorableDiscountRate;  // 优惠折扣(%)
    private BigDecimal favorableUnitPrice;     // 惠后单价
    private BigDecimal favorableAmount;        // 优惠后金额

    // ═══ 参考/成本 ═══
    private BigDecimal costPrice;              // 参考成本单价
    private BigDecimal costAmount;             // 参考成本金额
    private BigDecimal grossProfit;            // 参考毛利

    // ═══ 市场价格快照 ═══
    private BigDecimal retailPrice;            // 零售价
    private BigDecimal wholesalePrice;         // 批发价
    private BigDecimal minSalePrice;           // 最低售价
    private BigDecimal lastSalePrice;          // 最近售价
    private LocalDate lastSaleDate;            // 最近销售日期

    // ═══ 8个标准化价格等级 ═══
    private BigDecimal priceRestaurant;        // 餐饮店
    private BigDecimal priceCanteen;           // 食堂团餐
    private BigDecimal priceVipSelf;           // 自助VIP
    private BigDecimal priceLargeGroup;        // 大团餐
    private BigDecimal priceSpecialCustomer;   // 特价客户
    private BigDecimal priceOutRestaurant;     // 外围餐饮店
    private BigDecimal priceVipLevel1;         // 重点VIP01
    private BigDecimal priceVipLevel2;         // 连锁VIP

    // ═══ 库存快照 ═══
    private BigDecimal availableStock;              // 可用库存
    private BigDecimal availableStockConverted;     // 可用库存换算结果
    private BigDecimal bookStock;                   // 账面库存

    // ═══ 批次/保质期 ═══
    private String batchCode;                  // 批次编码
    private String batchNo;                    // 批次条码
    private LocalDate productionDate;          // 生产日期
    private String shelfLife;                  // 保质期
    private LocalDate expiryDate;              // 到期日期

    // ═══ 体积/重量 ═══
    private BigDecimal volume;                 // 体积(m³)
    private BigDecimal weight;                 // 重量(kg)

    // ═══ 赠品/兑换/积分 ═══
    private Boolean gift;                      // 赠品标记
    private String giftItem;                   // 兑换礼品
    private BigDecimal exchangePoints;         // 兑换积分
    private BigDecimal usedPoints;             // 使用积分
    private BigDecimal generatedPoints;        // 产生积分

    // ═══ 箱号 ═══
    private String boxNo;                      // 箱号
    private String customerTicket;             // 客户一票通

    // ═══ 备注 ═══
    private String remark;                     // 明细备注

    // ═══ 表体自定义字段（数字 1-7） ═══
    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private BigDecimal extNum3;
    private BigDecimal extNum4;
    private BigDecimal extNum5;
    private BigDecimal extNum6;
    private BigDecimal extNum7;

    // ═══ 表体自定义字段（文本 1-2） ═══
    private String extText1;
    private String extText2;

    // ═══ 表体自定义字段（往来单位/职员/部门） ═══
    private Long extPartner;
    private Long extStaff;
    private Long extDept;

    // ═══ 系统字段 ═══
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
