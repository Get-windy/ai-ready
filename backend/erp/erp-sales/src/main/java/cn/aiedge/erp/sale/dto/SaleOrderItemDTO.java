package cn.aiedge.erp.sale.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单明细DTO - 与实体保持一致，支持分布式存储
 */
@Data
public class SaleOrderItemDTO {

    private Long id;
    private Long orderId;
    private Integer lineNo;
    private Long productId;
    private String productCode;
    private String productName;
    private String preOrderNo;
    private String specification;
    private String model;
    private String location;
    private String batchCode;
    private LocalDateTime productionDate;
    private LocalDateTime expiryDate;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private BigDecimal costPrice;
    private BigDecimal originalPrice;
    private BigDecimal taxRate;
    private BigDecimal unitPriceWithTax;
    private BigDecimal amountWithTax;
    private BigDecimal discountRate;
    private BigDecimal discountPercent;
    private BigDecimal discountAmount;
    private BigDecimal usePreOrderAmount;
    private BigDecimal bigPack;
    private BigDecimal midPack;
    private BigDecimal smallPack;
    private String area;
    private String smallUnit;
    private BigDecimal smallUnitPrice;
    private BigDecimal smallUnitQuantity;
    private Boolean gift;
    private String giftItem;
    private BigDecimal exchangePoints;
    private BigDecimal usedPoints;
    private String remark;
    private Long warehouseId;

    // ===== 新增价格等级字段 =====
    private String customerGradeCode;
    private String customerGradeName;
    private String priceGradeCode;
    private String priceSource;
    private BigDecimal calculatedPrice;
    private String discountApplied;

    // ===== 自定义字段 =====
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

    // ===== JOIN获取字段 =====
    private String image;
    private String barcode;
    private String smallUnitBarcode;
    private String origin;
    private String brand;
    private String shelfLife;
    private BigDecimal retailPrice;
    private BigDecimal wholesalePrice;
    private String unit;
    private String lineAttribute;
    private BigDecimal volume;
    private BigDecimal weight;

    // ===== 计算字段 =====
    private BigDecimal availableStock;
    private BigDecimal availableStockConverted;
    private BigDecimal bookStock;
    private String conversionRelation;
    private LocalDateTime latestSaleDate;
    private BigDecimal latestSalePrice;
    private BigDecimal lowestPrice;
    private BigDecimal unshippedQuantity;
    private BigDecimal shippedQuantityDetail;
    private BigDecimal costAmount;
    private BigDecimal grossProfit;
    private BigDecimal discountedUnitPrice;
    private BigDecimal discountedAmount;
    private BigDecimal favorableUnitPrice;
    private BigDecimal favorableAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippedQuantity;
}