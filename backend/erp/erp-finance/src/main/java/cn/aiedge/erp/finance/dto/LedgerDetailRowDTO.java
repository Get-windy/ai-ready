package cn.aiedge.erp.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 明细账逐笔行（对标账簿 15 列）
 *
 * 首行为「期初余额」行（rowType=OPENING），其余为凭证分录取数行（rowType=ENTRY）。
 */
@Data
public class LedgerDetailRowDTO {

    /** 行类型 OPENING-期初余额 / ENTRY-凭证分录 */
    private String rowType;

    /** 分录ID（期初行无） */
    private Long id;

    /** 凭证ID */
    private Long voucherId;

    /** 单据编号（凭证号，期初行=「期初余额」） */
    private String voucherNo;

    /** 单据日期（凭证业务日期） */
    private LocalDate voucherDate;

    /** 记账日期（凭证过账日期，未过账回落单据日期） */
    private LocalDate postDate;

    /** 单据类型 manual-手工凭证 / system-系统凭证 */
    private String voucherType;

    /** 摘要（穿透业务单据：无分录摘要时由来源单据编号生成） */
    private String summary;

    /** 科目ID */
    private Long subjectId;

    /** 科目编码 */
    private String subjectCode;

    /** 科目名称 */
    private String subjectName;

    /** 核算单位 */
    private String auxUnit;

    /** 核算部门 */
    private String auxDept;

    /** 核算职员 */
    private String auxStaff;

    /** 借方发生额 */
    private BigDecimal debitAmount;

    /** 贷方发生额 */
    private BigDecimal creditAmount;

    /** 期末余额（借正贷负） */
    private BigDecimal balance;

    /** 余额方向 借/贷 */
    private String balanceDirection;

    /** 经手人 */
    private String handlerName;

    /** 部门 */
    private String deptName;

    /** 单据备注 */
    private String remark;

    /** 对账标记 0-未对账 1-已对账 */
    private Integer reconcileFlag;

    /** 来源业务类型 */
    private String sourceType;

    /** 来源业务编号 */
    private String sourceNo;
}
