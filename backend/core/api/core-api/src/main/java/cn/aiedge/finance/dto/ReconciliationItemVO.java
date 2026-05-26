package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对账明细项VO
 */
@Data
@Schema(description = "对账明细项详情")
public class ReconciliationItemVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "对账记录ID")
    private Long reconciliationId; // 对账记录ID

    @Schema(description = "明细项类型")
    private String itemType; // PAYABLE-应付账款 RECEIVABLE-应收账款 PAYMENT-付款记录 RECEIPT-收款记录

    @Schema(description = "明细项ID")
    private Long itemId; // 明细项ID（对应的应付/应收/付款/收款ID）

    @Schema(description = "明细项编号")
    private String itemNo; // 明细项编号

    @Schema(description = "系统记录金额")
    private BigDecimal systemAmount; // 系统记录金额

    @Schema(description = "实际金额")
    private BigDecimal actualAmount; // 实际金额

    @Schema(description = "差异金额")
    private BigDecimal difference; // 差异金额

    @Schema(description = "是否匹配")
    private Boolean matched; // 是否匹配

    @Schema(description = "差异原因")
    private String differenceReason; // 差异原因

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
