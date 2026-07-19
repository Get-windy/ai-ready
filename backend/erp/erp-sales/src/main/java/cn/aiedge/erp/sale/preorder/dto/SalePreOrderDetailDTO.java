package cn.aiedge.erp.sale.preorder.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预订货单明细视图DTO（用于"按明细"列表）
 * 包含明细表字段 + 关联的主表字段（通过JOIN获取）
 */
@Data
public class SalePreOrderDetailDTO {

    // ─── 明细表字段 ───
    private Long id;
    private Long tenantId;
    private Long orderId;
    private Integer lineNo;

    // 商品信息
    private Long productId;
    private String productName;
    private String productCode;
    private String barcode;
    private String imageUrl;
    private String specification;
    private String model;
    private String origin;
    private String brand;

    // 单位与换算
    private String unit;
    /** 计价单位 */
    private String pricingUnit;
    private String smallUnit;
    private BigDecimal smallUnitQuantity;
    private String conversionRelation;
    private BigDecimal conversionResult;

    // 库存
    private String region;
    private String location;
    private BigDecimal availableStock;
    private BigDecimal availableStockConversion;
    private BigDecimal bookStock;

    // 数量
    private BigDecimal quantity;
    private BigDecimal pieceQuantity;
    private BigDecimal bigPack;
    private BigDecimal midPack;
    private BigDecimal smallPack;
    private BigDecimal orderedQuantity;
    private BigDecimal unOrderedQuantity;
    private BigDecimal shippedQuantity;
    private BigDecimal unShippedQuantity;
    private BigDecimal terminateQuantity;
    private BigDecimal terminateAmount;

    // 价格
    private LocalDate lastSaleDate;
    private BigDecimal retailPrice;
    private BigDecimal wholesalePrice;
    private BigDecimal minSalePrice;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private BigDecimal smallUnitPrice;

    // 折扣
    private BigDecimal discountRate;
    private BigDecimal discountedPrice;
    private BigDecimal discountedAmount;

    // 成本与毛利
    private BigDecimal costPrice;
    private BigDecimal costAmount;
    private BigDecimal grossProfit;

    // 体积重量
    private BigDecimal volume;
    private BigDecimal weight;

    // 属性
    private String productAttribute;
    private Boolean gift;
    private String remark;

    // 价格等级(8个)
    private BigDecimal priceLevel1;
    private BigDecimal priceLevel2;
    private BigDecimal priceLevel3;
    private BigDecimal priceLevel4;
    private BigDecimal priceLevel5;
    private BigDecimal priceLevel6;
    private BigDecimal priceLevel7;
    private BigDecimal priceLevel8;

    // 扩展字段
    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private BigDecimal extNum3;
    private BigDecimal extNum4;
    private BigDecimal extNum5;
    private String extText1;
    private String extText2;
    private Long extPartner;
    private Long extStaff;
    private Long extDept;

    // ─── 主表字段（通过JOIN获取）───
    private String orderNo;
    private LocalDate orderDate;
    private Integer status;
    private Long customerId;
    private String customerName;
    private String customerCode;
    private String customerLevel;
    private Long warehouseId;
    private String warehouseName;
    private Long handlerId;
    private String handlerName;
    private String deptName;
    private Integer saleType;
    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;
    private String customerTicket;
    private String customerRemark;
    private String summary;
    private String attachment;
    private String orderRemark;
    private String creatorName;
    private String auditorName;
    private LocalDateTime submitTime;

    // 系统字段
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
