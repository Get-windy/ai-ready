package cn.aiedge.erp.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "对冲明细DTO")
public class OffsetItemDTO {

    @Schema(description = "方向: receivable(应收)/payable(应付)")
    @NotBlank(message = "方向不能为空")
    private String direction;

    @Schema(description = "关联单据类型: receipt/payment/receivable/payable")
    private String refType;

    @Schema(description = "关联单据ID")
    private Long refId;

    @Schema(description = "关联单据编号")
    private String refNo;

    @Schema(description = "参与对冲的金额")
    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "备注")
    private String remark;
}
