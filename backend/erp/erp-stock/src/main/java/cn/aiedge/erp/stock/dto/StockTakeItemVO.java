package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 盘点单「按明细」列表行：明细字段 + 单据级字段
 */
@Data
public class StockTakeItemVO {
    // ── 明细字段 ──
    private Long id;
    private Long stockTakeId;
    private Long productId;
    private String productCode;
    private String productName;
    private String productSpec;
    private String productUnit;
    private String barcode;
    private String model;
    private String origin;
    private String brand;
    private String region;
    private String location;
    private String image;
    private BigDecimal stockQuantity;
    private BigDecimal conversionResult;
    private BigDecimal checkQuantity;
    private BigDecimal checkQuantityConversionResult;
    private BigDecimal pieceQuantity;
    private BigDecimal diffQuantity;
    private LocalDate productionDate;
    private String conversionRelation;
    private String shelfLife;
    private LocalDate expiryDate;
    private String batchCode;
    private BigDecimal bigPack;
    private BigDecimal midPack;
    private BigDecimal smallPack;
    private Integer checkStatus;
    private BigDecimal costPrice;
    private BigDecimal diffConversionResult;
    private BigDecimal diffAmount;
    private BigDecimal itemExtNum1;
    private BigDecimal itemExtNum2;
    private BigDecimal itemExtNum3;
    private String itemExtText1;
    private String itemExtText2;
    private String remark;

    // ── 单据级字段 ──
    private LocalDate stockTakeDate;
    private String stockTakeNo;
    private Integer status;
    private Integer checkMethod;
    private Integer checkType;
    private Long warehouseId;
    private String warehouseName;
    private String regionName;
    private Long handlerId;
    private String handlerName;
    private String deptName;
    private String linkedBillNo;
    private String docRemark;
    private String summary;
    private String attachment;
    private String bookkeeperName;
    private String creatorName;
    private LocalDateTime bookkeepingTime;
    private LocalDateTime createTime;
    private Integer printCount;
}
