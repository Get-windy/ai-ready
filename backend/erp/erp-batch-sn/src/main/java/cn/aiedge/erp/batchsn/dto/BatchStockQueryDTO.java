package cn.aiedge.erp.batchsn.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * 批次库存查询DTO
 *
 * @author team-member
 * @date 2026-04-29
 */
@Data
public class BatchStockQueryDTO {

    private String batchNo;
    private String productCode;
    private String productName;
    private String batchStatus;
    private String qualityStatus;
    private Long warehouseId;
    private LocalDate productionDateStart;
    private LocalDate productionDateEnd;
    private LocalDate expirationDateStart;
    private LocalDate expirationDateEnd;
    private Boolean expiringOnly;
    private Integer warningDays;
    private int page = 1;
    private int size = 20;
}
