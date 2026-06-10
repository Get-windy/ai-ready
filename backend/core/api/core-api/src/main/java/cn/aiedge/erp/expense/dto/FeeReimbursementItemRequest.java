package cn.aiedge.erp.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 费用报销明细项请求
 */
@Data
@Schema(description = "费用报销明细项请求")
public class FeeReimbursementItemRequest {

    @Schema(description = "关联申请明细ID")
    private Long applicationItemId;

    @Schema(description = "费用项目名称")
    @NotBlank(message = "项目名称不能为空")
    private String itemName;

    @Schema(description = "费用说明")
    private String description;

    @Schema(description = "费用发生日期")
    @NotNull(message = "费用发生日期不能为空")
    private LocalDate expenseDate;

    @Schema(description = "金额")
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    @Schema(description = "数量")
    private BigDecimal quantity;

    @Schema(description = "单价")
    private BigDecimal unitPrice;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "供应商/收款方")
    private String vendorName;

    @Schema(description = "发票号码")
    private String invoiceNumber;

    @Schema(description = "项目编码")
    private String projectCode;

    @Schema(description = "成本中心")
    private String costCenter;

    @Schema(description = "序号")
    private Integer sequenceNumber;
}
