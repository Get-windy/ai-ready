package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 报损单「按明细」列表行：明细字段 + 单据级字段
 */
@Data
public class StockDamageItemVO {
    // ── 明细字段 ──
    private Long id;
    private Long damageId;
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
    private BigDecimal itemExtNum1;
    private BigDecimal itemExtNum2;
    private BigDecimal itemExtNum3;
    private String itemExtText1;
    private String itemExtText2;
    private String batchCode;
    private LocalDate productionDate;
    private String shelfLife;
    private LocalDate expiryDate;
    private BigDecimal quantity;
    private String conversionRelation;
    private BigDecimal conversionResult;
    private BigDecimal pieceQuantity;
    private BigDecimal bigPack;
    private BigDecimal midPack;
    private BigDecimal smallPack;
    private String smallUnit;
    private BigDecimal smallUnitQuantity;
    private BigDecimal unitCost;
    private BigDecimal smallUnitPrice;
    private BigDecimal amount;
    private BigDecimal weight;
    private BigDecimal volume;
    private String image;
    private String remark;

    // ── 单据级字段 ──
    private LocalDate damageDate;
    private String damageNo;
    private Integer status;
    private Long warehouseId;
    private String warehouseName;
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
}
