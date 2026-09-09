package cn.aiedge.erp.finance.cashtransfer.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 提存按明细列表行 VO
 * 展开主表头字段（单据日期/编号/状态/转出账户/经手人/部门/记账人/制单人/摘要/备注等）+ 转入账户明细字段。
 */
@Data
public class CashTransferItemVO {

    /** 明细ID */
    private Long id;

    /** 主表ID */
    private Long transferId;

    /** 行号 */
    private Integer lineNo;

    // ── 主表头字段（按单据列） ──
    private LocalDate docDate;

    private String docNo;

    private Integer status;

    private Long fromAccountId;

    private String fromAccountName;

    private BigDecimal fromAmount;

    private BigDecimal toAmount;

    private BigDecimal fee;

    private String handlerName;

    private String deptName;

    private String creatorName;

    private String bookkeeperName;

    private LocalDateTime bookkeepingTime;

    private String summary;

    private String remark;

    private LocalDateTime createTime;

    private Integer printCount;

    // ── 明细字段（转入账户） ──
    private Long toAccountId;

    private String toAccountNo;

    private String toAccountName;

    private Integer toAccountType;

    private String toSubjectCode;

    private BigDecimal amount;

    private String itemRemark;
}
