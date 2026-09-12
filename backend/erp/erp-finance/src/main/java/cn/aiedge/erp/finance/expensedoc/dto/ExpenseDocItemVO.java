package cn.aiedge.erp.finance.expensedoc.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 费用单按明细列表行 VO
 * 展开主表头字段（单据日期/编号/状态/往来单位/经手人/部门/制单人/记账人/摘要/备注等）+ 费用项明细字段。
 */
@Data
public class ExpenseDocItemVO {

    /** 明细ID */
    private Long id;

    /** 主表ID */
    private Long expenseDocId;

    /** 行号 */
    private Integer lineNo;

    // ── 主表头字段（按单据列） ──
    private LocalDate docDate;

    private String docNo;

    private Integer status;

    private Integer expenseType;

    private Integer approvalStatus;

    private Integer approvalLevel;

    private String currentApproverName;

    private Long partnerId;

    private String partnerCode;

    private String partnerName;

    private String handlerName;

    private String deptName;

    private String creatorName;

    private String auditorName;

    private String bookkeeperName;

    private LocalDateTime bookkeepingTime;

    private String summary;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer printCount;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    private String payAccountName;

    // ── 明细字段（费用项） ──
    private String expenseCode;

    private String expenseName;

    private String subjectCode;

    private String subjectName;

    private BigDecimal amount;

    private String itemRemark;
}
