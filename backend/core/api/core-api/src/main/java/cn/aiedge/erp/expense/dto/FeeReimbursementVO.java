package cn.aiedge.erp.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 费用报销详情VO
 */
@Data
@Schema(description = "费用报销详情")
public class FeeReimbursementVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "报销单号")
    private String reimbursementNo;

    @Schema(description = "报销标题")
    private String reimbursementTitle;

    @Schema(description = "报销人ID")
    private Long applicantId;

    @Schema(description = "报销人姓名")
    private String applicantName;

    @Schema(description = "部门ID")
    private Long departmentId;

    @Schema(description = "部门名称")
    private String departmentName;

    @Schema(description = "关联费用申请ID")
    private Long applicationId;

    @Schema(description = "关联申请单号")
    private String applicationNo;

    @Schema(description = "报销总金额")
    private BigDecimal totalAmount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "报销日期")
    private LocalDate reimbursementDate;

    @Schema(description = "报销事由")
    private String purpose;

    @Schema(description = "详细说明")
    private String description;

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

    @Schema(description = "支付方式")
    private String paymentMethod;

    @Schema(description = "付款状态")
    private String paymentStatus;

    @Schema(description = "已付款金额")
    private BigDecimal paidAmount;

    @Schema(description = "付款日期")
    private LocalDate paymentDate;

    @Schema(description = "付款凭证号")
    private String paymentVoucherNo;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人")
    private Long createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "明细项")
    private List<FeeReimbursementItemVO> items;

    @Schema(description = "审批记录")
    private List<FeeApprovalRecordVO> approvalRecords;
}
