package cn.aiedge.erp.fixedasset.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 固定资产盘点DTO
 */
@Data
public class FixedAssetInventoryDTO {
    private Long id;
    private String inventoryNo;
    private LocalDate inventoryDate;
    private String departmentId;
    private String departmentName;
    private Long assetId;
    private String assetCode;
    private String assetName;
    private String expectedLocation;
    private String actualLocation;
    private String expectedStatus;
    private String actualStatus;
    private String expectedCustodian;
    private String actualCustodian;
    private String checkResult;
    private String remark;
    private String status;
}
