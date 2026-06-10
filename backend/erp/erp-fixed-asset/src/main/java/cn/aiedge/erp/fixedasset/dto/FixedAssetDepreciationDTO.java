package cn.aiedge.erp.fixedasset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 固定资产折旧记录DTO
 */
@Data
@Schema(description = "固定资产折旧记录")
public class FixedAssetDepreciationDTO {

    @Schema(description = "ID")
    private Long id;

    @NotNull(message = "资产ID不能为空")
    @Schema(description = "资产ID")
    private Long assetId;

    @NotBlank(message = "期间不能为空")
    @Schema(description = "期间（yyyy-MM）")
    private String period;

    @Schema(description = "折旧日期")
    private LocalDate depreciationDate;

    @Schema(description = "本期折旧金额")
    private BigDecimal periodAmount;

    @Schema(description = "累计折旧金额")
    private BigDecimal accumulatedDepreciation;

    @Schema(description = "净值")
    private BigDecimal netValue;

    @Schema(description = "资产原值")
    private BigDecimal assetOriginalValue;

    @Schema(description = "资产名称")
    private String assetName;

    @Schema(description = "资产编码")
    private String assetCode;

    @Schema(description = "状态")
    private String status;

    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(description = "备注")
    private String remark;
}
