package cn.aiedge.erp.purchase.purchasereturn.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PurchaseReturnItemDTO {

    private Long id;

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    private String image;

    private String barcode;

    private String model;

    private String origin;

    private String brand;

    private String region;

    private String location;

    private BigDecimal availableStock;
    private BigDecimal availableStockConverted;
    private BigDecimal bookStock;

    private String batchNo;

    private String batchCode;

    private LocalDate productionDate;

    private String shelfLife;

    private LocalDate expiryDate;

    private BigDecimal returnQuantity;

    private String conversionRelation;

    private BigDecimal pieceQuantity;
    private BigDecimal bigPack;
    private BigDecimal midPack;
    private BigDecimal smallPack;

    private LocalDate latestPurchaseDate;

    private BigDecimal retailPrice;
    private BigDecimal wholesalePrice;
    private BigDecimal unitPrice;

    private String smallUnit;
    private BigDecimal smallUnitPrice;
    private BigDecimal smallUnitQuantity;

    private BigDecimal unitCost;

    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal lineTotal;

    private BigDecimal volume;
    private BigDecimal weight;

    private Boolean gift;

    private Boolean restaurant;
    private Boolean canteen;
    private Boolean outRestaurant;
    private Boolean vipSelf;
    private Boolean largeGroup;
    private Boolean vipLevel1;
    private Boolean vipLevel2;
    private Boolean specialCustomer;

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

    private String reason;

    private String remark;
}
