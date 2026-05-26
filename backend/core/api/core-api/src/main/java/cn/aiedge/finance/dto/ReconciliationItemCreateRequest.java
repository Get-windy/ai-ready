package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 对账明细项创建请求
 */
@Data
@Schema(description = "对账明细项创建请求")
public class ReconciliationItemCreateRequest {

    @Schema(description = "明细项类型", required = true)
    @NotBlank(message = "明细项类型不能为空")
    private String itemType; // PAYABLE-应付账款 RECEIVABLE-应收账款 PAYMENT-付款记录 RECEIPT-收款记录

    @Schema(description = "明细项ID", required = true)
    @NotNull(message = "明细项ID不能为空")
    private Long itemId; // 明细项ID（对应的应付/应收/付款/收款ID）

    @Schema(description = "明细项编号")
    private String itemNo; // 明细项编号

    @Schema(description = "系统记录金额", required = true)
    @NotNull(message = "系统记录金额不能为空")
    @DecimalMin(value = "0.00", message = "系统记录金额不能小于0")
    private BigDecimal systemAmount; // 系统记录金额

    @Schema(description = "实际金额", required = true)
    @NotNull(message = "实际金额不能为空")
    @DecimalMin(value = "0.00", message = "实际金额不能小于0")
    private BigDecimal actualAmount; // 实际金额

    @Schema(description = "是否匹配")
    private Boolean matched; // 是否匹配

    @Schema(description = "差异原因")
    private String differenceReason; // 差异原因

    @Schema(description = "备注")
    private String remark;
}
