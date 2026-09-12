package cn.aiedge.erp.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 账款交账 - 五档统计卡片 + 底部合计
 * 口径：待交账单据=张数；待交账金额=应收合计；待交账收款金额=使用预收+收款合计；
 *      待交账优惠=收款优惠；待交账欠款金额=应收-实收-优惠
 */
@Data
@Schema(description = "账款交账 五档统计DTO")
public class AccountDeliverySummaryDTO {

    @Schema(description = "待交账单据（张数）")
    private Long docCount = 0L;

    @Schema(description = "待交账金额")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Schema(description = "待交账收款金额")
    private BigDecimal receiveAmount = BigDecimal.ZERO;

    @Schema(description = "待交账收款金额-使用预收")
    private BigDecimal usedAdvance = BigDecimal.ZERO;

    @Schema(description = "待交账收款金额-收款合计")
    private BigDecimal receiveTotal = BigDecimal.ZERO;

    @Schema(description = "待交账优惠")
    private BigDecimal favorableAmount = BigDecimal.ZERO;

    @Schema(description = "待交账欠款金额")
    private BigDecimal arrearsAmount = BigDecimal.ZERO;

    public void accumulate(Long count, java.math.BigDecimal amount, java.math.BigDecimal advance,
                           java.math.BigDecimal receive, java.math.BigDecimal favorable, java.math.BigDecimal arrears) {
        this.docCount += (count == null ? 0 : count);
        this.totalAmount = this.totalAmount.add(nullToZero(amount));
        this.usedAdvance = this.usedAdvance.add(nullToZero(advance));
        this.receiveTotal = this.receiveTotal.add(nullToZero(receive));
        this.favorableAmount = this.favorableAmount.add(nullToZero(favorable));
        this.arrearsAmount = this.arrearsAmount.add(nullToZero(arrears));
        this.receiveAmount = this.usedAdvance.add(this.receiveTotal);
    }

    private java.math.BigDecimal nullToZero(java.math.BigDecimal v) {
        return v == null ? java.math.BigDecimal.ZERO : v;
    }
}
