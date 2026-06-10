package cn.aiedge.erp.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 费用统计VO
 */
@Data
@Schema(description = "费用统计")
public class FeeStatisticsVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "统计年份")
    private Integer statYear;

    @Schema(description = "统计月份")
    private Integer statMonth;

    @Schema(description = "统计日期")
    private LocalDate statDate;

    @Schema(description = "部门ID")
    private Long departmentId;

    @Schema(description = "部门名称")
    private String departmentName;

    @Schema(description = "费用类型")
    private String expenseType;

    @Schema(description = "申请数量")
    private Integer applyCount;

    @Schema(description = "申请金额")
    private BigDecimal applyAmount;

    @Schema(description = "已审批数量")
    private Integer approvedCount;

    @Schema(description = "已审批金额")
    private BigDecimal approvedAmount;

    @Schema(description = "已拒绝数量")
    private Integer rejectedCount;

    @Schema(description = "已拒绝金额")
    private BigDecimal rejectedAmount;

    @Schema(description = "报销数量")
    private Integer reimbursementCount;

    @Schema(description = "报销金额")
    private BigDecimal reimbursementAmount;

    @Schema(description = "已付款数量")
    private Integer paidCount;

    @Schema(description = "已付款金额")
    private BigDecimal paidAmount;

    @Schema(description = "预算金额")
    private BigDecimal budgetAmount;

    @Schema(description = "预算使用率(%)")
    private BigDecimal budgetUsageRate;
}
