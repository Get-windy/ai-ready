package cn.aiedge.erp.finance.initial.vo;

import lombok.Data;

/**
 * 「当前会计年」取值结果（{@code GET /api/erp/finance/initial/current-year}）。
 *
 * <p>取代前端原先写死的 {@code new Date().getFullYear()}（自然年）—— 自然年与会计年
 * 可能不一致（开发文档 §5.3 已登记该口径风险 P2）。</p>
 *
 * <p><b>回退链</b>（按顺序，见 InitialFinanceServiceImpl#currentYear）：
 * <ol>
 *   <li>{@code fin_accounting_period} 中 {@code status = 1} 且「今天落在 start_date..end_date 之间」
 *       的那一行所属的 {@code period_year} → {@link #SOURCE_OPEN_PERIOD}；</li>
 *   <li>找不到则取该租户最大的 {@code period_year} → {@link #SOURCE_MAX_PERIOD_YEAR}；</li>
 *   <li>再没有（该租户零期间）则回退服务器系统年 → {@link #SOURCE_SYSTEM_YEAR}，同时给出提示文案。</li>
 * </ol>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class CurrentYearVO {

    /** 取值来源：命中了「开启中且今天落在期间内」的会计期间 */
    public static final String SOURCE_OPEN_PERIOD = "OPEN_PERIOD";
    /** 取值来源：回退到该租户最大的会计年度 */
    public static final String SOURCE_MAX_PERIOD_YEAR = "MAX_PERIOD_YEAR";
    /** 取值来源：回退到服务器系统年（该租户没有任何会计期间） */
    public static final String SOURCE_SYSTEM_YEAR = "SYSTEM_YEAR";

    /** 当前会计年（即回退链的最终取值；前端用作新增/录入的默认年度） */
    private Integer periodYear;

    /** 服务器系统年（自然年），供前端对比展示 */
    private Integer systemYear;

    /** 取值来源：OPEN_PERIOD / MAX_PERIOD_YEAR / SYSTEM_YEAR */
    private String source;

    /** 提示文案（仅在回退到系统年等需要提醒的场景有值，其余为 null） */
    private String hint;
}
