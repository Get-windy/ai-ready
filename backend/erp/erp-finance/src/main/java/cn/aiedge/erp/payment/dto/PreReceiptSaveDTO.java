package cn.aiedge.erp.payment.dto;

import cn.aiedge.erp.payment.entity.PreReceiptItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 预收款单保存/记账请求（含收款账户明细）
 */
@Data
@Schema(description = "预收款单保存请求")
public class PreReceiptSaveDTO {

    @Schema(description = "ID（更新时传）")
    private Long id;

    @Schema(description = "预收款单号")
    private String preReceiptNo;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "来源业务ID")
    private Long sourceId;

    @Schema(description = "来源业务编号（源单）")
    private String sourceNo;

    @Schema(description = "源单未结金额")
    private BigDecimal sourceUnsettledAmount;

    @Schema(description = "结算单位编号（客户编号）")
    private String partnerCode;

    @NotNull(message = "结算单位不能为空")
    @Schema(description = "客户ID（结算单位）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long customerId;

    @Schema(description = "客户名称（结算单位）")
    private String customerName;

    @Schema(description = "本次预收金额（Σ收款明细）")
    private BigDecimal amount;

    @Schema(description = "本单赠送金额")
    private BigDecimal giftAmount;

    @Schema(description = "此前预收快照")
    private BigDecimal prevAmount;

    @Schema(description = "类型")
    private String depositType;

    @Schema(description = "单据日期")
    private LocalDate receiptDate;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "经手人ID")
    private Long handlerId;

    @Schema(description = "经手人")
    private String handlerName;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门")
    private String deptName;

    @Schema(description = "支付方式")
    private Integer paymentMethod;

    @Schema(description = "银行账号")
    private String bankAccount;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "交易流水号")
    private String transactionNo;

    @Schema(description = "收款账户明细")
    private List<PreReceiptItem> items;
}
