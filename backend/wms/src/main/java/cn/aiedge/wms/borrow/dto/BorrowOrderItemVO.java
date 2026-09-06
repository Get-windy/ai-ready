package cn.aiedge.wms.borrow.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借进借出单「按明细」列表行：明细字段 + 单据级字段 + 往来单位+商品主数据字段
 */
@Data
public class BorrowOrderItemVO {

    // ── 明细字段 ──
    private Long id;
    private Long orderId;
    private Long lineNo;
    private Long productId;
    private String productCode;
    private String productName;
    private String productSpec;
    private String barcode;
    private String model;
    private String origin;
    private String region;
    private String location;
    private String taste;
    private String unit;
    private String smallUnit;
    private BigDecimal smallUnitQuantity;
    private BigDecimal smallUnitPrice;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal returnedQuantity;
    private BigDecimal processedReturnQuantity;
    private BigDecimal processedPurchaseQuantity;
    private BigDecimal nonProcessedQuantity;
    private BigDecimal nonProcessedAmount;
    private String conversionRelation;
    private BigDecimal conversionResult;
    private BigDecimal pieceQuantity;
    private BigDecimal bigPack;
    private BigDecimal midPack;
    private BigDecimal smallPack;
    private BigDecimal retailPrice;
    private BigDecimal wholesalePrice;
    private BigDecimal minPrice;
    private LocalDate productionDate;
    private String shelfLife;
    private LocalDate expiryDate;
    private String batchCode;
    private BigDecimal availableStock;
    private BigDecimal bookStock;
    private BigDecimal weight;
    private BigDecimal volume;
    private BigDecimal priceLevel1;
    private BigDecimal priceLevel2;
    private BigDecimal priceLevel3;
    private BigDecimal priceLevel4;
    private BigDecimal priceLevel5;
    private BigDecimal priceLevel6;
    private BigDecimal priceLevel7;
    private BigDecimal priceLevel8;
    private BigDecimal itemExtNum1;
    private BigDecimal itemExtNum2;
    private BigDecimal itemExtNum3;
    private String itemExtText1;
    private String itemExtText2;
    private String remark;

    // ── 单据级字段 ──
    private LocalDate borrowDate;
    private String orderNo;
    private Integer status;
    private Long warehouseId;
    private String warehouseName;
    private Long partnerId;
    private String partnerCode;
    private String partnerName;
    private String handlerName;
    private String deptName;
    private String docRemark;
    private String summary;
    private String attachment;
    private String bookkeeperName;
    private String creatorName;
    private LocalDateTime bookkeepingTime;
    private LocalDateTime createTime;
    private Integer printCount;
    private LocalDate expectedReturnDate;
    private BigDecimal totalWeight;
    private BigDecimal totalVolume;

    // ── 往来单位主数据字段（若来源数据有则带出） ──
    private String customerLevel;
    private String contact;
    private String address;
    private String defaultHandler;
    private String oneBill;
    private String customerRemark;
}
