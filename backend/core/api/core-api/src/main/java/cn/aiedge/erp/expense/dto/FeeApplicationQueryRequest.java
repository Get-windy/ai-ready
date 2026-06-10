package cn.aiedge.erp.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 费用申请查询请求
 */
@Data
@Schema(description = "费用申请查询请求")
public class FeeApplicationQueryRequest {

    @Schema(description = "申请单号")
    private String applicationNo;

    @Schema(description = "申请标题(模糊)")
    private String applicationTitle;

    @Schema(description = "申请人ID")
    private Long applicantId;

    @Schema(description = "部门ID")
    private Long departmentId;

    @Schema(description = "费用类型")
    private String expenseType;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "申请日期起")
    private LocalDate applyDateStart;

    @Schema(description = "申请日期止")
    private LocalDate applyDateEnd;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 20;
}
