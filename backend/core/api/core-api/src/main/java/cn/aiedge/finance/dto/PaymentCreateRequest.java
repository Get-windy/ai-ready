package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 付款记录创建请求
 */
@Data
@Schema(description = "付款记录创建请求")
public class PaymentCreateRequest {

    @Schema(description = "应付账款ID", required = true)
    @NotNull(message = "应付账款ID不能为空")
    private Long payableId;

    @Schema(description = "付款金额", required = true)
    @NotNull(message = "付款金额不能为空")
    @DecimalMin(value = "0.01", message = "付款金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "付款日期", required = true)
    @NotNull(message = "付款日期不能为空")
    private LocalDate paymentDate;

    @Schema(description = "付款方式")
    private String paymentMethod;

    @Schema(description = "银行账号")
    private String bankAccount;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "凭证号")
    private String voucherNo;

    @Schema(description = "备注")
    private String remark;
}
