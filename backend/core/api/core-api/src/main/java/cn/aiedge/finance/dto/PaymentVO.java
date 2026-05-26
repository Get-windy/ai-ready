package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 付款记录VO
 */
@Data
@Schema(description = "付款记录详情")
public class PaymentVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "付款编号")
    private String paymentNo;

    @Schema(description = "应付账款ID")
    private Long payableId;

    @Schema(description = "供应商ID")
    private Long supplierId;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "付款金额")
    private BigDecimal amount;

    @Schema(description = "付款日期")
    private LocalDate paymentDate;

    @Schema(description = "付款方式")
    private String paymentMethod;

    @Schema(description = "银行账号")
    private String bankAccount;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "凭证号")
    private String voucherNo;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
