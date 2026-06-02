package cn.aiedge.erp.fixedasset.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 固定资产转移DTO
 */
@Data
public class FixedAssetTransferDTO {
    private Long id;
    private String transferNo;
    private Long assetId;
    private String assetCode;
    private String assetName;
    private String fromDepartmentId;
    private String fromDepartmentName;
    private String toDepartmentId;
    private String toDepartmentName;
    private String fromCustodianId;
    private String fromCustodianName;
    private String toCustodianId;
    private String toCustodianName;
    private LocalDate transferDate;
    private String reason;
    private String status;
    private String approvalComment;
    private LocalDateTime transferTime;
    private String remark;
}
