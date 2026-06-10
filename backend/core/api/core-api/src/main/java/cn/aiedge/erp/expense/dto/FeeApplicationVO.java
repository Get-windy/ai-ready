package cn.aiedge.erp.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 费用申请详情VO
 */
@Data
@Schema(description = "费用申请详情")
public class FeeApplicationVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "申请单号")
    private String applicationNo;

    @Schema(description = "申请标题")
    private String applicationTitle;

    @Schema(description = "申请人ID")
    private Long applicantId;

    @Schema(description = "申请人姓名")
    private String applicantName;

    @Schema(description = "部门ID")
    private Long departmentId;

    @Schema(description = "部门名称")
    private String departmentName;

    @Schema(description = "费用类型")
    private String expenseType;

    @Schema(description = "费用类型描述")
    private String expenseTypeDesc;

    @Schema(description = "费用总金额")
    private BigDecimal totalAmount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "预算金额")
    private BigDecimal budgetAmount;

    @Schema(description = "预算使用率")
    private BigDecimal budgetUsageRate;

    @Schema(description = "是否超出预算")
    private Integer exceedBudget;

    @Schema(description = "超出金额")
    private BigDecimal exceedAmount;

    @Schema(description = "申请日期")
    private LocalDate applyDate;

    @Schema(description = "费用事由")
    private String purpose;

    @Schema(description = "详细说明")
    private String description;

    @Schema(description = "是否紧急")
    private Integer isUrgent;

    @Schema(description = "紧急原因")
    private String urgentReason;

    @Schema(description = "预计完成日期")
    private LocalDate expectedCompletionDate;

    @Schema(description = "实际完成日期")
    private LocalDate actualCompletionDate;

    @Schema(description = "附件数量")
    private Integer attachmentCount;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "当前审批人ID")
    private Long currentApproverId;

    @Schema(description = "当前审批人姓名")
    private String currentApproverName;

    @Schema(description = "当前审批级别")
    private Integer currentApprovalLevel;

    @Schema(description = "总审批级别数")
    private Integer totalApprovalLevel;

    @Schema(description = "审批意见")
    private String approvalComment;

    @Schema(description = "拒绝原因")
    private String rejectReason;

    @Schema(description = "报销状态")
    private String reimbursementStatus;

    @Schema(description = "已报销金额")
    private BigDecimal reimbursedAmount;

    @Schema(description = "报销日期")
    private LocalDate reimbursementDate;

    @Schema(description = "付款状态")
    private String paymentStatus;

    @Schema(description = "已付款金额")
    private BigDecimal paidAmount;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人")
    private Long createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "明细项")
    private List<FeeApplicationItemVO> items;

    @Schema(description = "审批记录")
    private List<FeeApprovalRecordVO> approvalRecords;
}
