package cn.aiedge.erp.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "资金流水DTO")
public class CapitalFlowDTO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "流水号")
    private String flowNo;

    @Schema(description = "流水类型: receipt/payment/pre_receipt/pre_payment/offset/transfer")
    private String flowType;

    @Schema(description = "方向: IN/OUT")
    private String direction;

    @Schema(description = "关联单据ID")
    private Long refId;

    @Schema(description = "关联单据编号")
    private String refNo;

    @Schema(description = "关联单据类型")
    private String refType;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "账户余额")
    private BigDecimal balance;

    @Schema(description = "对方类型: customer/supplier/internal")
    private String partyType;

    @Schema(description = "对方ID")
    private Long partyId;

    @Schema(description = "对方名称")
    private String partyName;

    @Schema(description = "业务类型: sale/purchase/expense/asset/other")
    private String businessType;

    @Schema(description = "发生时间")
    private LocalDateTime occurDate;

    @Schema(description = "支付方式")
    private Integer paymentMethod;

    @Schema(description = "银行账号")
    private String bankAccount;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "交易流水号")
    private String transactionNo;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
