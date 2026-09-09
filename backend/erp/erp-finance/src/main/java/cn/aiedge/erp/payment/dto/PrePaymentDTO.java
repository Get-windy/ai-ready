package cn.aiedge.erp.payment.dto;

import cn.aiedge.erp.payment.entity.PrePaymentItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "预付款/定金DTO")
public class PrePaymentDTO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "预付款单号")
    private String prePaymentNo;

    @Schema(description = "来源类型: purchase_deposit/rental_deposit/bid_bond/other")
    private String sourceType;

    @Schema(description = "来源业务ID")
    private Long sourceId;

    @Schema(description = "来源业务编号")
    private String sourceNo;

    @Schema(description = "源单未结金额")
    private BigDecimal sourceUnsettledAmount;

    @Schema(description = "供应商编号（结算单位编号）")
    private String partnerCode;

    @Schema(description = "供应商ID（结算单位）")
    private Long supplierId;

    @Schema(description = "供应商名称（结算单位）")
    private String supplierName;

    @Schema(description = "本次预付金额（Σ付款明细）")
    private BigDecimal amount;

    @Schema(description = "已冲抵金额")
    private BigDecimal usedAmount;

    @Schema(description = "剩余金额")
    private BigDecimal remainingAmount;

    @Schema(description = "此前预付（记账前预付余额快照）")
    private BigDecimal prevAmount;

    @Schema(description = "类型: deposit(定金)/deposit_guarantee(押金)/bid_bond(保证金)")
    private String depositType;

    @Schema(description = "单据日期")
    private LocalDate paymentDate;

    @Schema(description = "状态: draft/confirmed/offset/recovered/refunded")
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

    @Schema(description = "附件")
    private String attachment;

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

    @Schema(description = "付款账户明细")
    private List<PrePaymentItem> items;
}
