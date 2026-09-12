package cn.aiedge.erp.finance.expensedoc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 费用单主表实体
 * 非主营支出费用登记（往来单位费用/内部费用两类），与其他收入单对称。
 * 单号前缀 YBFYD-。状态 0-草稿 1-已记账 2-已取消。
 * P0 红线：记账必须经会计凭证（KJPZ-），严禁直接改费用/往来余额。
 */
@Data
@Accessors(chain = true)
@TableName("erp_expense_doc")
public class ExpenseDoc {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 单号 YBFYD-YYYYMMDD-序号 */
    private String docNo;

    /** 单据日期 */
    private LocalDate docDate;

    /** 费用类型 0-往来单位费用 1-内部费用 */
    private Integer expenseType;

    /** 往来单位ID（往来单位费用） */
    private Long partnerId;

    /** 往来编号 */
    private String partnerCode;

    /** 往来单位名称 */
    private String partnerName;

    /** 经手人 */
    private Long handlerId;

    private String handlerName;

    /** 部门（内部费用） */
    private Long deptId;

    private String deptName;

    /** 付款账户1 */
    private Long payAccountId;

    private String payAccountName;

    /** 付款账户1科目编码(凭证用) */
    private String paySubjectCode;

    private BigDecimal payAmount;

    /** 付款账户2 */
    private Long payAccount2Id;

    private String payAccount2Name;

    private String paySubjectCode2;

    private BigDecimal payAmount2;

    /** 付款账户3 */
    private Long payAccount3Id;

    private String payAccount3Name;

    private String paySubjectCode3;

    private BigDecimal payAmount3;

    /** 付款账户4 */
    private Long payAccount4Id;

    private String payAccount4Name;

    private String paySubjectCode4;

    private BigDecimal payAmount4;

    /** 摘要 */
    private String summary;

    /** 单据备注 */
    private String remark;

    /** 本单金额 = Σ费用项金额 = Σ付款金额 */
    private BigDecimal totalAmount;

    /** 状态 0-草稿 1-已记账 2-已取消 */
    private Integer status;

    /** 制单人 */
    private String creatorName;

    /** 记账人 */
    private Long bookkeeperId;

    private String bookkeeperName;

    /** 记账时间 */
    private LocalDateTime bookkeepingTime;

    /** 审批状态 0-未提交 1-审批中 2-审批通过 3-审批驳回 */
    private Integer approvalStatus;

    /** 当前审批级别 1-部门 2-财务 3-总经理 */
    private Integer approvalLevel;

    /** 审批总级数 */
    private Integer totalApprovalLevel;

    /** 当前审批人ID（待办隔离口径） */
    private Long currentApproverId;

    /** 当前审批人 */
    private String currentApproverName;

    /** 提交审批时间 */
    private LocalDateTime submitTime;

    /** 驳回原因 */
    private String rejectReason;

    /** 打印次数 */
    private Integer printCount;

    /** 红冲标记 */
    private Integer redFlag;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @Version
    private Integer versionNo;
}
