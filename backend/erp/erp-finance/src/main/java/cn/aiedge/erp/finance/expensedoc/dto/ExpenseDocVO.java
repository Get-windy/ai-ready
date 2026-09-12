package cn.aiedge.erp.finance.expensedoc.dto;

import cn.aiedge.erp.finance.expensedoc.entity.ExpenseItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 费用单详情 VO（含费用项明细 items）
 */
@Data
public class ExpenseDocVO {

    private Long id;

    private Long tenantId;

    private String docNo;

    private LocalDate docDate;

    private Integer expenseType;

    private Long partnerId;

    private String partnerCode;

    private String partnerName;

    private Long handlerId;

    private String handlerName;

    private Long deptId;

    private String deptName;

    private Long payAccountId;

    private String payAccountName;

    private String paySubjectCode;

    private BigDecimal payAmount;

    private Long payAccount2Id;

    private String payAccount2Name;

    private String paySubjectCode2;

    private BigDecimal payAmount2;

    private Long payAccount3Id;

    private String payAccount3Name;

    private String paySubjectCode3;

    private BigDecimal payAmount3;

    private Long payAccount4Id;

    private String payAccount4Name;

    private String paySubjectCode4;

    private BigDecimal payAmount4;

    private String summary;

    private String remark;

    private BigDecimal totalAmount;

    private Integer status;

    private String creatorName;

    private Long bookkeeperId;

    private String bookkeeperName;

    private LocalDateTime bookkeepingTime;

    private Integer printCount;

    private Integer redFlag;

    /** 审批状态 0-未提交 1-审批中 2-审批通过 3-审批驳回 */
    private Integer approvalStatus;

    /** 当前审批级别 1-部门 2-财务 3-总经理 */
    private Integer approvalLevel;

    /** 审批总级数 */
    private Integer totalApprovalLevel;

    /** 当前审批人ID */
    private Long currentApproverId;

    /** 当前审批人 */
    private String currentApproverName;

    /** 提交审批时间 */
    private LocalDateTime submitTime;

    /** 驳回原因 */
    private String rejectReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 费用项明细 */
    private List<ExpenseItem> items;
}
