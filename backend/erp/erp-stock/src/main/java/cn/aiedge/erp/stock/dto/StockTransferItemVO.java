package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 调拨单「按明细」列表行：明细字段 + 单据级字段
 */
@Data
public class StockTransferItemVO {

    // ── 明细字段 ──
    private Long id;
    private Long transferId;
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
    private BigDecimal bookStock;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal costAmount;
    private BigDecimal transferPrice;
    private BigDecimal transferAmount;
    private BigDecimal transferDiff;
    private BigDecimal weight;
    private BigDecimal volume;
    private String batchCode;
    private String batchNo;
    private LocalDate productionDate;
    private LocalDate validityDate;

    // ── 表体/单据自定义 ──
    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private BigDecimal extNum3;
    private String extText1;
    private String extText2;
    private BigDecimal docCustom1;
    private BigDecimal docCustom2;
    private BigDecimal docCustom3;
    private String docCustom4;
    private String docCustom5;

    private String remark;

    // ── 单据级字段 ──
    private LocalDate billDate;
    private String transferNo;
    private Integer status;
    private Integer transferType;
    private Long fromWarehouseId;
    private String fromWarehouseName;
    private Long toWarehouseId;
    private String toWarehouseName;
    private String applicationName;
    private String handlerName;
    private String departmentName;
    private String sourceBillNo;
    private String docRemark;
    private String summary;
    private Integer attachment;
    private String posterName;
    private String createByName;
    private LocalDateTime posterTime;
    private LocalDateTime createTime;
    private Integer printCount;
}
