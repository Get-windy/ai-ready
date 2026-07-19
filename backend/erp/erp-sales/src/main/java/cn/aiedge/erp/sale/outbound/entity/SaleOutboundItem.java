package cn.aiedge.erp.sale.outbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售出库明细实体
 * 遵循主从表快照模型：存外键ID + 快照字段（成交后锁定不变）
 * 预留 ext_num/ext_text/ext_partner/ext_staff/ext_dept 自定义扩展字段
 */
@Data
@Accessors(chain = true)
@TableName("erp_sale_outbound_item")
public class SaleOutboundItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long outboundId;

    private Integer lineNo;

    // ═══ 产品快照 ═══
    private Long productId;
    private String productCode;       // 货号
    private String productName;
    private String imageUrl;          // 图片URL
    private String barcode;           // 条码
    private String smallUnitBarcode;  // 小单位条码
    private String specification;     // 规格
    private String productSpec;       // 产品规格（兼容字段）
    private String model;             // 型号
    private String origin;            // 产地
    private String brand;             // 品牌
    private String productUnit;       // 计价单位（兼容字段）
    private String productAttribute;  // 商品行属性

    // ═══ 货位/区域 ═══
    private String location;          // 货位
    @TableField(exist = false)
    private String region;            // 区域
    private Integer warehouseLocationId;
    private String warehouseLocationCode;

    // ══ 来源订单 ═══
    private Long orderItemId;
    private String orderNo;           // 来源订单编号
    private BigDecimal orderQuantity; // 订单数量

    // ═══ 数量 ═══
    private BigDecimal quantity;           // 出库数量（前端字段名）
    private BigDecimal outboundQuantity;   // 出库数量（后端字段名）
    private BigDecimal pendingQuantity;    // 待处理数量
    private BigDecimal pieceQuantity;      // 件散数量

    // ═══ 包装 ══
    private BigDecimal bigPack;        // 大包装
    private BigDecimal midPack;        // 中包装
    private BigDecimal smallPack;      // 小包装

    // ═══ 单位换算 ═══
    private String conversionRelation;   // 换算关系
    private BigDecimal conversionResult; // 换算结果
    private String smallUnit;            // 小单位
    private BigDecimal smallUnitQuantity;// 小单位数量

    // ══ 价格快照 ═══
    private BigDecimal unitPrice;            // 单价
    private BigDecimal lineAmount;           // 金额
    private BigDecimal originalPrice;        // 折单原价
    private BigDecimal smallUnitPrice;       // 小单位单价
    private BigDecimal taxRate;              // 税率

    // ═══ 折扣 ═══
    private BigDecimal discountRate;         // 折扣(%)
    private BigDecimal discountedAmount;     // 折后金额
    private BigDecimal discountedPrice;      // 折后单价
    private BigDecimal favorableDiscountRate;// 优惠折扣(%)
    private BigDecimal favorableUnitPrice;   // 惠后单价
    private BigDecimal favorableAmount;      // 优惠后金额

    // ═══ 参考/成本 ═══
    private BigDecimal costPrice;        // 参考成本单价
    private BigDecimal costAmount;       // 参考成本金额
    private BigDecimal grossProfit;      // 参考毛利

    // ═══ 市场价格快照 ═══
    private BigDecimal retailPrice;          // 零售价
    private BigDecimal wholesalePrice;       // 批发价
    private BigDecimal minSalePrice;         // 最低售价
    private BigDecimal lastSalePrice;        // 最近售价
    private LocalDate lastSaleDate;          // 最近销售日期

    // ═══ 价格等级（8级） ═══
    private BigDecimal priceLevel1;
    private BigDecimal priceLevel2;
    private BigDecimal priceLevel3;
    private BigDecimal priceLevel4;
    private BigDecimal priceLevel5;
    private BigDecimal priceLevel6;
    private BigDecimal priceLevel7;
    private BigDecimal priceLevel8;

    // ═══ 库存快照 ═══
    private BigDecimal availableStock;       // 可用库存
    private BigDecimal availableStockConverted; // 可用库存换算结果
    private BigDecimal bookStock;            // 账面库存

    // ═══ 批次/保质期 ═══
    private String shelfLife;                // 保质期
    private String batchNo;                  // 批次条码
    private LocalDateTime productionDate;    // 生产日期
    private LocalDateTime validityDate;      // 到期日期（兼容性）
    private LocalDate expiryDate;            // 到期日期

    // ═══ 体积/重量 ═══
    private BigDecimal volume;   // 体积(m³)
    private BigDecimal weight;   // 重量(kg)

    // ═══ 赠品/兑换/积分 ═══
    private Boolean gift;                    // 赠品
    private String giftItem;                 // 兑换礼品
    private BigDecimal exchangePoints;       // 兑换积分
    private BigDecimal usedPoints;           // 使用积分
    private BigDecimal generatedPoints;      // 产生积分

    // ═══ 箱号 ═══
    private String boxNo;
    private String customerTicket;     // 客户一票通

    // ═══ 拣货/打包流程 ═══
    private Integer pickingStatus;
    private Long pickingBy;
    private LocalDateTime pickingTime;
    private Integer packingStatus;
    private Long packingBy;
    private LocalDateTime packingTime;

    // ═══ 备注 ═══
    private String remark;

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
