package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 收款记录创建请求
 */
@Data
@Schema(description = "收款记录创建请求")
public class ReceiptCreateRequest {

    @Schema(description = "应收ID", required = true)
    private Long receivableId;

    @Schema(description = "收款金额", required = true)
    private BigDecimal amount;

    @Schema(description = "收款日期", required = true)
    private LocalDate receiptDate;

    @Schema(description = "付款方式")
    private String paymentMethod;

    @Schema(description = "银行账户")
    private String bankAccount;

    @Schema(description = "凭证号")
    private String voucherNo;

    @Schema(description = "备注")
    private String remark;
}
