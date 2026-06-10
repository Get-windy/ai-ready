package cn.aiedge.erp.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 付款记录VO
 */
@Data
@Schema(description = "付款记录")
public class FeePaymentRecordVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务单据ID")
    private Long businessId;

    @Schema(description = "业务单据编号")
    private String businessNo;

    @Schema(description = "付款单号")
    private String paymentNo;

    @Schema(description = "付款金额")
    private BigDecimal paymentAmount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "支付方式")
    private String paymentMethod;

    @Schema(description = "付款账户")
    private String paymentAccount;

    @Schema(description = "收款人名称")
    private String payeeName;

    @Schema(description = "收款人账户")
    private String payeeAccount;

    @Schema(description = "付款日期")
    private LocalDate paymentDate;

    @Schema(description = "凭证号")
    private String voucherNo;

    @Schema(description = "付款人")
    private String payerName;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "确认人")
    private String confirmUserName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
