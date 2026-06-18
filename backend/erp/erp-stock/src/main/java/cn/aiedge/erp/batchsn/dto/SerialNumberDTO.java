package cn.aiedge.erp.batchsn.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 序列号DTO
 *
 * @author team-member
 * @date 2026-04-29
 */
@Data
public class SerialNumberDTO {

    private Long id;
    private String serialNo;
    private Long productId;
    private String productCode;
    private String productName;
    private String specification;
    private Long batchId;
    private String batchNo;
    private String snStatus;
    private String snStatusName;
    private String snStage;
    private String manufacturer;
    private LocalDate manufacturingDate;
    private Integer warrantyPeriod;
    private LocalDate warrantyEndDate;
    private String currentLocation;
    private Long warehouseId;
    private String saleOrderNo;
    private String qualityStatus;
    private Integer maintenanceCount;
    private LocalDateTime lastMaintenanceDate;
    private String createdByName;
    private LocalDateTime createdAt;
}
