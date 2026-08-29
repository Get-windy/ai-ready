package cn.aiedge.erp.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 明细账条目DTO（按科目逐笔凭证分录）
 */
@Data
public class LedgerDetailDTO {
    private Long id;
    /** 凭证ID */
    private Long voucherId;
    /** 凭证号（联查） */
    private String voucherNo;
    /** 凭证日期（联查） */
    private LocalDate voucherDate;
    /** 摘要 */
    private String summary;
    /** 会计科目ID */
    private Long subjectId;
    /** 科目编码 */
    private String subjectCode;
    /** 科目名称 */
    private String subjectName;
    /** 借方金额 */
    private BigDecimal debitAmount;
    /** 贷方金额 */
    private BigDecimal creditAmount;
    /** 来源业务类型 */
    private String sourceType;
    /** 来源业务编号 */
    private String sourceNo;
}
