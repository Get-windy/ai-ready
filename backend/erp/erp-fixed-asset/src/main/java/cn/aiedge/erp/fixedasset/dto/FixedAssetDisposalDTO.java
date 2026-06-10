package cn.aiedge.erp.fixedasset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 固定资产处置DTO
 */
@Data
@Schema(description = "固定资产处置")
public class FixedAssetDisposalDTO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "处置单号")
    private String disposalNo;

    @NotNull(message = "资产ID不能为空")
    @Schema(description = "资产ID")
    private Long assetId;

    @Schema(description = "资产编码")
    private String assetCode;

    @Schema(description = "资产名称")
    private String assetName;

    @Schema(description = "处置日期")
    private LocalDate disposalDate;

    @NotBlank(message = "处置类型不能为空")
    @Schema(description = "处置类型")
    private String disposalType;

    @Schema(description = "处置金额")
    private BigDecimal disposalAmount;

    @Schema(description = "净值")
    private BigDecimal netValue;

    @Schema(description = "处置损益")
    private BigDecimal gainLoss;

    @NotBlank(message = "处置原因不能为空")
    @Size(max = 1000, message = "处置原因长度不能超过1000")
    @Schema(description = "处置原因")
    private String reason;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "审批意见")
    private String approvalComment;

    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(description = "备注")
    private String remark;
}
