package cn.aiedge.erp.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "创建预收款/定金请求")
public class PreReceiptCreateDTO {

    @Schema(description = "来源类型: sale_deposit/rental_deposit/bid_bond/other")
    private String sourceType;

    @Schema(description = "来源业务ID")
    private Long sourceId;

    @Schema(description = "来源业务编号")
    private String sourceNo;

    @NotNull(message = "客户ID不能为空")
    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @NotNull(message = "预收金额不能为空")
    @Positive(message = "预收金额必须大于0")
    @Schema(description = "预收金额", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Schema(description = "类型: deposit(定金)/deposit_guarantee(押金)/bid_bond(保证金)")
    private String depositType;

    @Schema(description = "收款日期")
    private LocalDate receiptDate;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "支付方式")
    private Integer paymentMethod;

    @Schema(description = "银行账号")
    private String bankAccount;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "交易流水号")
    private String transactionNo;
}
