package cn.aiedge.erp.payment.dto;

import cn.aiedge.erp.payment.entity.PreReceiptItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    @Schema(description = "源单未结金额")
    private BigDecimal sourceUnsettledAmount;

    @Schema(description = "结算单位编号（客户编号）")
    private String partnerCode;

    @Schema(description = "客户ID（结算单位）")
    private Long customerId;

    @Schema(description = "客户名称（结算单位）")
    private String customerName;

    @Schema(description = "本次预收金额（Σ收款明细）")
    private BigDecimal amount;

    @Schema(description = "已冲抵金额")
    private BigDecimal usedAmount;

    @Schema(description = "剩余金额")
    private BigDecimal remainingAmount;

    @Schema(description = "本单赠送金额（计入预收不计资金）")
    private BigDecimal giftAmount;

    @Schema(description = "总金额 = 本次预收 + 赠送金额")
    private BigDecimal totalAmount;

    @Schema(description = "此前预收（记账前预收余额快照）")
    private BigDecimal prevAmount;

    @Schema(description = "类型: deposit(定金)/deposit_guarantee(押金)/bid_bond(保证金)")
    private String depositType;

    @Schema(description = "单据日期")
    private LocalDate receiptDate;

    @Schema(description = "状态: draft/confirmed/offset/forfeited/refunded")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "经手人ID")
    private Long handlerId;

    @Schema(description = "经手人")
    private String handlerName;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门")
    private String deptName;

    @Schema(description = "制单人")
    private String creatorName;

    @Schema(description = "记账人ID")
    private Long bookkeeperId;

    @Schema(description = "记账人")
    private String bookkeeperName;

    @Schema(description = "记账时间")
    private LocalDateTime bookkeepingTime;

    @Schema(description = "审核人ID")
    private Long auditorId;

    @Schema(description = "审核人")
    private String auditorName;

    @Schema(description = "审核时间")
    private LocalDateTime auditorTime;

    @Schema(description = "打印次数")
    private Integer printCount;

    @Schema(description = "支付方式")
    private Integer paymentMethod;

    @Schema(description = "银行账号")
    private String bankAccount;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "交易流水号")
    private String transactionNo;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "收款账户明细")
    private List<PreReceiptItem> items;
}
