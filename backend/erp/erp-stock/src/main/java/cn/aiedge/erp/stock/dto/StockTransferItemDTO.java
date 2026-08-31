package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockTransferItemDTO {

    private Long productId;
    private String productCode;
    private String productName;
    private String productSpec;
    private String productUnit;

    private String image;
    private String model;
    private String origin;
    private String brand;
    private String region;
    private String locationOut;
    private String locationIn;

    private String barcode;
    private String shelfLife;
    private String conversionRelation;
    private BigDecimal conversionResult;
    private BigDecimal pieceQuantity;
    private BigDecimal bigPack;
    private BigDecimal midPack;
    private BigDecimal smallPack;

    private String smallUnit;
    private BigDecimal smallUnitPrice;
    private BigDecimal smallUnitQuantity;

    private BigDecimal availableStock;
    private BigDecimal availableStockConverted;
    private BigDecimal bookStock;

    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal costAmount;

    private BigDecimal transferPrice;
    private BigDecimal transferAmount;
    private BigDecimal transferDiff;

    private BigDecimal weight;
    private BigDecimal volume;
    private BigDecimal retailPrice;
    private BigDecimal wholesalePrice;

    private String batchNo;
    private String batchCode;
    private LocalDateTime productionDate;
    private LocalDateTime validityDate;

    /** 表体自定义1~3(数字)/4~5(文本) */
    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private BigDecimal extNum3;
    private String extText1;
    private String extText2;

    /** 单据自定义1~3(数字)/4~5(文本) */
    private BigDecimal docCustom1;
    private BigDecimal docCustom2;
    private BigDecimal docCustom3;
    private String docCustom4;
    private String docCustom5;

    private String remark;
}
