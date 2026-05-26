package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应付账款创建请求
 */
@Data
@Schema(description = "应付账款创建请求")
public class PayableCreateRequest {

    @Schema(description = "供应商ID", required = true)
    @NotNull(message = "供应商ID不能为空")
    private Long supplierId;

    @Schema(description = "合同ID")
    private Long contractId;

    @Schema(description = "原始金额", required = true)
    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    private BigDecimal originalAmount;

    @Schema(description = "开票日期", required = true)
    @NotNull(message = "开票日期不能为空")
    private LocalDate billDate;

    @Schema(description = "到期日期", required = true)
    @NotNull(message = "到期日期不能为空")
    private LocalDate dueDate;

    @Schema(description = "备注")
    private String remark;
}
