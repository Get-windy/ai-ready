package cn.aiedge.erp.sale.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售订单明细DTO - 完整字段版
 * 对标生产级ERP系统，覆盖所有76个业务字段
 */
@Data
public class SaleOrderItemDTO {

    private Long id;
    private Long orderId;
    private Integer lineNo;

    // ═══ 商品信息 ═══
    private Long productId;
    private String productCode;
    private String productName;
    private String image;
    private String itemCode;
    private String barcode;
    private String smallUnitBarcode;
    private String specification;
    private String model;
    private String origin;
    private String brand;
    private String shelfLife;
    private String unit;
    private String pricingUnit;
    private String smallUnit;
    private String lineAttribute;
    private String area;
    private String location;

    // ══ 批次信息 ═══
    private LocalDate productionDate;
    private LocalDate expiryDate;
    private String batchCode;

    // ═══ 包装/数量 ═══
    private BigDecimal quantity;
    private BigDecimal bigPack;
    private BigDecimal midPack;
    private BigDecimal smallPack;
    private BigDecimal smallUnitQuantity;

    // ═══ 库存相关 ═══
    private BigDecimal availableStock;
    private BigDecimal availableStockConverted;
    private BigDecimal bookStock;
    private String conversionRelation;
    private BigDecimal unshippedQuantity;
    private BigDecimal shippedQuantityDetail;

    // ═══ 价格信息 ═══
    private BigDecimal smallUnitPrice;
    private LocalDate latestSaleDate;
    private BigDecimal latestSalePrice;
    private BigDecimal retailPrice;
    private BigDecimal wholesalePrice;
    private BigDecimal lowestPrice;
    private BigDecimal unitPrice;
    private BigDecimal costPrice;

    // ═══ 客户类型（价格等级标准化字段） ═══
    private Boolean restaurant;
    private Boolean canteen;
    private Boolean vipSelf;
    private Boolean largeGroup;
    private Boolean specialCustomer;
    private Boolean outRestaurant;
    private Boolean vipLevel1;
    private Boolean vipLevel2;

    // ═══ 预订货 ═══
    private String preOrderNo;
    private BigDecimal usePreOrderAmount;

    // ═══ 折扣 ═══
    private BigDecimal discountRate;
    private BigDecimal discountPercent;
    private BigDecimal originalPrice;
    private BigDecimal discountedUnitPrice;
    private BigDecimal favorableUnitPrice;

    // ═══ 积分/礼品 ═══
    private String giftItem;
    private BigDecimal exchangePoints;
    private BigDecimal usedPoints;

    // ═══ 物理属性 ═══
    private BigDecimal volume;
    private BigDecimal weight;

    // ═══ 赠品 ═══
    private Boolean gift;

    // ═══ 备注 ══
    private String remark;

    // ══ 仓库 ═══
    private Long warehouseId;

    // ═══ 价格等级（快照） ═══
    private String customerGradeCode;
    private String customerGradeName;
    private String priceGradeCode;
    private String priceSource;
    private BigDecimal calculatedPrice;
    private String discountApplied;

    // ═══ 自定义字段 ══
    private BigDecimal customField1;
    private BigDecimal customField2;
    private BigDecimal customField3;
    private String customField4;
    private String customField5;
    private BigDecimal customField6;
    private BigDecimal customField7;
    private Long customField8;
    private Long customField9;
    private Long customField10;

    // ═══ 计算字段 ═══
    private BigDecimal amount;
    private BigDecimal costAmount;
    private BigDecimal grossProfit;
    private BigDecimal discountedAmount;
    private BigDecimal favorableAmount;
    private BigDecimal taxAmount;
    private BigDecimal unitPriceWithTax;
    private BigDecimal amountWithTax;
    private BigDecimal discountAmount;
    private BigDecimal taxRate;
    private BigDecimal shippedQuantity;
}
