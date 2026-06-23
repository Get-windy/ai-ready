package cn.aiedge.erp.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "预收款/定金DTO")
public class PreReceiptDTO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "预收款单号")
    private String preReceiptNo;

    @Schema(description = "来源类型: sale_deposit/rental_deposit/bid_bond/other")
    private String sourceType;

    @Schema(description = "来源业务ID")
    private Long sourceId;

    @Schema(description = "来源业务编号")
    private String sourceNo;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "预收金额")
    private BigDecimal amount;

    @Schema(description = "已冲抵金额")
    private BigDecimal usedAmount;

    @Schema(description = "剩余金额")
    private BigDecimal remainingAmount;

    @Schema(description = "类型: deposit(定金)/deposit_guarantee(押金)/bid_bond(保证金)")
    private String depositType;

    @Schema(description = "收款日期")
    private LocalDate receiptDate;

    @Schema(description = "状态: received/offset/forfeited/refunded")
    private String status;

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

    @Schema(description = "创建时间")
    private java.time.LocalDateTime createTime;
}
