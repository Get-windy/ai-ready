package cn.aiedge.erp.payment.dto;

import cn.aiedge.erp.payment.entity.PrePaymentItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 预付款单保存/记账请求（含付款账户明细）
 */
@Data
@Schema(description = "预付款单保存请求")
public class PrePaymentSaveDTO {

    @Schema(description = "ID（更新时传）")
    private Long id;

    @Schema(description = "预付款单号")
    private String prePaymentNo;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "来源业务ID")
    private Long sourceId;

    @Schema(description = "来源业务编号（源单）")
    private String sourceNo;

    @Schema(description = "源单未结金额")
    private BigDecimal sourceUnsettledAmount;

    @Schema(description = "供应商编号（结算单位编号）")
    private String partnerCode;

    @NotNull(message = "结算单位不能为空")
    @Schema(description = "供应商ID（结算单位）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long supplierId;

    @Schema(description = "供应商名称（结算单位）")
    private String supplierName;

    @Schema(description = "本次预付金额（Σ付款明细）")
    private BigDecimal amount;

    @Schema(description = "此前预付快照")
    private BigDecimal prevAmount;

    @Schema(description = "类型")
    private String depositType;

    @Schema(description = "单据日期")
    private LocalDate paymentDate;

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

    @Schema(description = "付款账户明细")
    private List<PrePaymentItem> items;
}
