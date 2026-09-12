package cn.aiedge.erp.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 明细账分页结果
 *
 * 口径：首行期初余额 + 逐笔凭证分录（含逐笔期末余额），列尾合计（借方/贷方/期末余额）。
 * 余额 = 期初 + Σ借 − Σ贷，逐笔可追溯（对应开发文档 P0 账簿口径红线）。
 */
@Data
public class LedgerDetailPageDTO {

    /** 当前页行（含期初余额行） */
    private List<LedgerDetailRowDTO> records;

    /** 总行数（含期初余额行） */
    private long total;

    /** 期初余额（借正贷负） */
    private BigDecimal openingBalance;

    /** 期初余额方向 借/贷 */
    private String openingDirection;

    /** 借方发生额合计（查询区间内全部明细） */
    private BigDecimal totalDebit;

    /** 贷方发生额合计（查询区间内全部明细） */
    private BigDecimal totalCredit;

    /** 期末余额（期初 + Σ借 − Σ贷） */
    private BigDecimal closingBalance;

    /** 期末余额方向 借/贷 */
    private String closingDirection;
}
