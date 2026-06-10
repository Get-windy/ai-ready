package cn.aiedge.erp.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "创建往来对冲请求")
public class OffsetCreateDTO {

    @Schema(description = "对方类型: customer/supplier")
    @NotBlank(message = "对方类型不能为空")
    private String partyType;

    @Schema(description = "对方ID")
    @NotNull(message = "对方ID不能为空")
    private Long partyId;

    @Schema(description = "对方名称")
    private String partyName;

    @Schema(description = "对冲金额")
    @NotNull(message = "对冲金额不能为空")
    @Positive(message = "对冲金额必须大于0")
    private BigDecimal offsetAmount;

    @Schema(description = "对冲日期")
    private LocalDate offsetDate;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "对冲明细")
    @NotNull(message = "对冲明细不能为空")
    private List<OffsetItemDTO> items;
}
