package cn.aiedge.erp.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 资产负债表报表结果（左右对照）
 *
 * 口径：期末余额 = 所选期止月的时点数；年初余额 = 本年度年初数。
 * 平衡：资产合计 = 负债合计 + 所有者权益合计（所有权者权益含本年损益，未结转时也应平衡）。
 */
@Data
public class BalanceSheetReportDTO {

    // ── 查询口径回显 ──
    private Integer fiscalYear;
    private Integer fiscalPeriod;
    private String periodMode;
    private Integer startPeriod;
    private Integer endPeriod;
    private Integer subjectLevel;
    private Boolean showZero;

    /** 左右对照数据行 */
    private List<BalanceSheetRowDTO> rows = new ArrayList<>();

    // ── 合计 ──
    private BigDecimal assetBeginTotal = BigDecimal.ZERO;
    private BigDecimal assetEndTotal = BigDecimal.ZERO;
    private BigDecimal liabilityBeginTotal = BigDecimal.ZERO;
    private BigDecimal liabilityEndTotal = BigDecimal.ZERO;
    private BigDecimal equityBeginTotal = BigDecimal.ZERO;
    private BigDecimal equityEndTotal = BigDecimal.ZERO;
    private BigDecimal liabilityEquityBeginTotal = BigDecimal.ZERO;
    private BigDecimal liabilityEquityEndTotal = BigDecimal.ZERO;

    /** 是否平衡（资产合计 = 负债合计 + 所有者权益合计） */
    private Boolean balanced = Boolean.FALSE;
    /** 平衡差额（资产合计 - 负债和所有者权益总计） */
    private BigDecimal difference = BigDecimal.ZERO;
    /** 是否取到科目余额数据 */
    private Boolean hasData = Boolean.FALSE;
}
