package cn.aiedge.erp.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 付款创建请求
 */
@Data
@Schema(description = "付款创建请求")
public class FeePaymentCreateRequest {

    @Schema(description = "业务类型: APPLICATION/REIMBURSEMENT")
    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    @Schema(description = "业务单据ID")
    @NotNull(message = "业务单据ID不能为空")
    private Long businessId;

    @Schema(description = "付款金额")
    @NotNull(message = "付款金额不能为空")
    private BigDecimal paymentAmount;

    @Schema(description = "支付方式")
    private String paymentMethod;

    @Schema(description = "付款账户")
    private String paymentAccount;

    @Schema(description = "付款账户名称")
    private String accountName;

    @Schema(description = "收款人名称")
    @NotBlank(message = "收款人不能为空")
    private String payeeName;

    @Schema(description = "收款人账户")
    private String payeeAccount;

    @Schema(description = "付款日期")
    @NotNull(message = "付款日期不能为空")
    private LocalDate paymentDate;

    @Schema(description = "凭证号")
    private String voucherNo;

    @Schema(description = "备注")
    private String remark;
}
