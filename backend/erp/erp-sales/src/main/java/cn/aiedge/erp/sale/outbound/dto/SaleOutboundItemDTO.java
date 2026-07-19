package cn.aiedge.erp.sale.outbound.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售出库明细DTO（前端 → 后端）
 * @JsonAlias 兼容前端字段名差异
 */
@Data
public class SaleOutboundItemDTO {

    private Long productId;

    private String productCode;

    private String productName;

    private String imageUrl;

    private String barcode;

    private String smallUnitBarcode;

    @JsonAlias("specification")
    private String productSpec;

    private String specification;  // 前端直接传 specification

    private String model;

    private String origin;

    private String brand;

    @JsonAlias("unit")
    private String productUnit;

    private String productAttribute;

    private String location;

    private String region;

    private Long orderItemId;

    private String orderNo;

    private BigDecimal orderQuantity;

    private BigDecimal quantity;

    private BigDecimal outboundQuantity;

    private BigDecimal pendingQuantity;

    private BigDecimal pieceQuantity;

    private BigDecimal bigPack;

    private BigDecimal midPack;

    private BigDecimal smallPack;

    private String conversionRelation;

    private BigDecimal conversionResult;

    private String smallUnit;

    private BigDecimal smallUnitQuantity;

    private BigDecimal unitPrice;

    @JsonAlias("amount")
    private BigDecimal lineAmount;

    private BigDecimal amount;  // 前端直接传 amount

    private BigDecimal smallUnitPrice;

    private BigDecimal taxRate;

    private BigDecimal discountRate;

    private BigDecimal discountedAmount;

    private BigDecimal discountedPrice;

    private BigDecimal favorableDiscountRate;

    private BigDecimal favorableUnitPrice;

    private BigDecimal favorableAmount;

    private BigDecimal costPrice;

    private BigDecimal costAmount;

    private BigDecimal grossProfit;

    private BigDecimal retailPrice;

    private BigDecimal wholesalePrice;

    private BigDecimal minSalePrice;

    private BigDecimal lastSalePrice;

    private String lastSaleDate;

    private BigDecimal priceLevel1;
    private BigDecimal priceLevel2;
    private BigDecimal priceLevel3;
    private BigDecimal priceLevel4;
    private BigDecimal priceLevel5;
    private BigDecimal priceLevel6;
    private BigDecimal priceLevel7;
    private BigDecimal priceLevel8;

    private BigDecimal availableStock;

    private BigDecimal bookStock;

    private String shelfLife;

    private String batchNo;

    private String productionDate;

    private String expiryDate;

    private BigDecimal volume;

    private BigDecimal weight;

    private Boolean gift;

    private String giftItem;

    private BigDecimal exchangePoints;

    private BigDecimal usedPoints;

    private BigDecimal generatedPoints;

    private String boxNo;

    private String remark;

    // 表体自定义字段
    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private BigDecimal extNum3;
    private BigDecimal extNum4;
    private BigDecimal extNum5;
    private BigDecimal extNum6;
    private BigDecimal extNum7;
    private String extText1;
    private String extText2;
    private Long extPartner;
    private Long extStaff;
    private Long extDept;
}
