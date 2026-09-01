package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 成本调价单「按明细」列表行：明细字段 + 单据级字段
 */
@Data
public class StockCostAdjustItemVO {

    // ── 明细字段 ──
    private Long id;
    private Long adjustId;
    private Long productId;
    private String productCode;
    private String productName;
    private String productSpec;
    private String productUnit;
    private String barcode;
    private String location;
    private String taste;
    private String model;
    private String origin;
    private String brand;
    private BigDecimal currentQuantity;
    private BigDecimal oldCost;
    private BigDecimal oldAmount;
    private BigDecimal newCost;
    private BigDecimal newAmount;
    private BigDecimal diffAmount;
    private String remark;

    // ── 单据级字段 ──
    private LocalDate adjustDate;
    private String adjustNo;
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
