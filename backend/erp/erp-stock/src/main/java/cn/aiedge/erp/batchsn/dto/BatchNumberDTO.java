package cn.aiedge.erp.batchsn.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 批次号DTO
 *
 * @author team-member
 * @date 2026-04-29
 */
@Data
public class BatchNumberDTO {

    private Long id;
    private String batchNo;
    private Long productId;
    private String productCode;
    private String productName;
    private String specification;
    private String unit;
    private LocalDate productionDate;
    private LocalDate expirationDate;
    private String batchStatus;
    private String batchStatusName;
    private BigDecimal totalQuantity;
    private BigDecimal availableQuantity;
    private BigDecimal reservedQuantity;
    private String sourceType;
    private String sourceRefNo;
    private Long warehouseId;
    private String warehouseName;
    private Long locationId;
    private String qualityStatus;
    private String qualityInspectorName;
    private LocalDateTime qualityInspectionDate;
    private String createdByName;
    private LocalDateTime createdAt;
}
