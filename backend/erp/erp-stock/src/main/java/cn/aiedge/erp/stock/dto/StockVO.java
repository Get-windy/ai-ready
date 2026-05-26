package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockVO {

    private Long id;

    private Long productId;

    private String productName;

    private String productCode;

    private Long warehouseId;

    private String warehouseName;

    private BigDecimal quantity;

    private BigDecimal availableQuantity;

    private BigDecimal frozenQuantity;

    private BigDecimal safetyStock;

    private BigDecimal minStock;

    private BigDecimal maxStock;

    private String unit;

    private String batchNo;

    private LocalDateTime productionDate;

    private LocalDateTime validityDate;

    private Long supplierId;

    private String supplierName;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    private Boolean isLowStock;

    private Boolean isOverStock;

    private Boolean isExpiring;
}