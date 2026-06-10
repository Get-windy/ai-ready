package cn.aiedge.erp.fixedasset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 固定资产转移DTO
 */
@Data
@Schema(description = "固定资产转移")
public class FixedAssetTransferDTO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "转移单号")
    private String transferNo;

    @NotNull(message = "资产ID不能为空")
    @Schema(description = "资产ID")
    private Long assetId;

    @Schema(description = "资产编码")
    private String assetCode;

    @Schema(description = "资产名称")
    private String assetName;

    @Schema(description = "转出部门ID")
    private String fromDepartmentId;

    @Schema(description = "转出部门名称")
    private String fromDepartmentName;

    @Schema(description = "转入部门ID")
    private String toDepartmentId;

    @Schema(description = "转入部门名称")
    private String toDepartmentName;

    @Schema(description = "转出保管人ID")
    private String fromCustodianId;

    @Schema(description = "转出保管人姓名")
    private String fromCustodianName;

    @Schema(description = "转入保管人ID")
    private String toCustodianId;

    @Schema(description = "转入保管人姓名")
    private String toCustodianName;

    @Schema(description = "转移日期")
    private LocalDate transferDate;

    @Size(max = 1000, message = "转移原因长度不能超过1000")
    @Schema(description = "转移原因")
    private String reason;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "审批意见")
    private String approvalComment;

    @Schema(description = "转移时间")
    private LocalDateTime transferTime;

    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(description = "备注")
    private String remark;
}
