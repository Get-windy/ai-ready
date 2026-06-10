package cn.aiedge.erp.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 费用报销创建请求
 */
@Data
@Schema(description = "费用报销创建请求")
public class FeeReimbursementCreateRequest {

    @Schema(description = "报销标题")
    @NotBlank(message = "报销标题不能为空")
    private String reimbursementTitle;

    @Schema(description = "关联费用申请ID")
    private Long applicationId;

    @Schema(description = "报销日期")
    @NotNull(message = "报销日期不能为空")
    private LocalDate reimbursementDate;

    @Schema(description = "报销事由")
    private String purpose;

    @Schema(description = "详细说明")
    private String description;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "明细项")
    @NotEmpty(message = "至少需要一条明细")
    private List<FeeReimbursementItemRequest> items;

    @Schema(description = "部门ID")
    private Long departmentId;

    @Schema(description = "部门名称")
    private String departmentName;
}
