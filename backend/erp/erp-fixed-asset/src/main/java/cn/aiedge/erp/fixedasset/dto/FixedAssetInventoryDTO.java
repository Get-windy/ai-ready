package cn.aiedge.erp.fixedasset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 固定资产盘点DTO
 */
@Data
@Schema(description = "固定资产盘点")
public class FixedAssetInventoryDTO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "盘点单号")
    private String inventoryNo;

    @Schema(description = "盘点日期")
    private LocalDate inventoryDate;

    @Schema(description = "部门ID")
    private String departmentId;

    @Schema(description = "部门名称")
    private String departmentName;

    @NotNull(message = "资产ID不能为空")
    @Schema(description = "资产ID")
    private Long assetId;

    @Schema(description = "资产编码")
    private String assetCode;

    @Schema(description = "资产名称")
    private String assetName;

    @Schema(description = "期望存放地点")
    private String expectedLocation;

    @Schema(description = "实际存放地点")
    private String actualLocation;

    @Schema(description = "期望状态")
    private String expectedStatus;

    @Schema(description = "实际状态")
    private String actualStatus;

    @Schema(description = "期望保管人")
    private String expectedCustodian;

    @Schema(description = "实际保管人")
    private String actualCustodian;

    @Schema(description = "盘点结果")
    private String checkResult;

    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态")
    private String status;
}
