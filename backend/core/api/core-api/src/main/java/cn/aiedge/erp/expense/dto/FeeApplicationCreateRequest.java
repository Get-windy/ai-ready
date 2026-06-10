package cn.aiedge.erp.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 费用申请创建请求
 */
@Data
@Schema(description = "费用申请创建请求")
public class FeeApplicationCreateRequest {

    @Schema(description = "申请标题")
    @NotBlank(message = "申请标题不能为空")
    private String applicationTitle;

    @Schema(description = "费用类型")
    @NotBlank(message = "费用类型不能为空")
    private String expenseType;

    @Schema(description = "申请日期")
    @NotNull(message = "申请日期不能为空")
    private LocalDate applyDate;

    @Schema(description = "费用事由")
    @NotBlank(message = "费用事由不能为空")
    private String purpose;

    @Schema(description = "详细说明")
    private String description;

    @Schema(description = "是否紧急")
    private Integer isUrgent;

    @Schema(description = "紧急原因")
    private String urgentReason;

    @Schema(description = "预计完成日期")
    private LocalDate expectedCompletionDate;

    @Schema(description = "预算金额")
    private BigDecimal budgetAmount;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "明细项")
    @NotEmpty(message = "至少需要一条明细")
    private List<FeeApplicationItemRequest> items;

    @Schema(description = "部门ID")
    private Long departmentId;

    @Schema(description = "部门名称")
    private String departmentName;
}
