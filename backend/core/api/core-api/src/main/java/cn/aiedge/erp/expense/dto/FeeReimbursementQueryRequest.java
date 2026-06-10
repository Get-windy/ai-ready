package cn.aiedge.erp.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;

/**
 * 费用报销查询请求
 */
@Data
@Schema(description = "费用报销查询请求")
public class FeeReimbursementQueryRequest {

    @Schema(description = "报销单号")
    private String reimbursementNo;

    @Schema(description = "报销标题(模糊)")
    private String reimbursementTitle;

    @Schema(description = "报销人ID")
    private Long applicantId;

    @Schema(description = "部门ID")
    private Long departmentId;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "关联申请ID")
    private Long applicationId;

    @Schema(description = "报销日期起")
    private LocalDate reimbursementDateStart;

    @Schema(description = "报销日期止")
    private LocalDate reimbursementDateEnd;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 20;
}
