package cn.aiedge.erp.finance.initial.dto;

import lombok.Data;

/**
 * 财务期初查询条件（分页/导出口径一致）。
 *
 * <p>查询字段按 Tab 维度分派：按科目的 3 个 Tab（银行现金/固定资产/资产负债）用
 * {@code subjectCode}/{@code subjectName}；按往来单位的 2 个 Tab（应付/应收）用
 * {@code partnerCode}/{@code partnerName} —— 与对标 ql361「按 Tab 切换查询字段」一致
 * （开发文档 §3.3 目标规格）。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class InitialFinanceQuery {

    /** 页码，从 1 开始 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 20;

    /**
     * 期初类型（**必传**，用于区分 Tab，避免各 Tab 串数据）：
     * 按科目表取 BANK_CASH / FIXED_ASSET / BALANCE_SHEET；
     * 按往来表取 PAYABLE / RECEIVABLE。
     */
    private String initialType;

    /** 期初年度（不传 = 不按年度过滤，展示该 Tab 全部年度） */
    private Integer periodYear;

    /** 科目编号（模糊） */
    private String subjectCode;

    /** 科目名称（模糊） */
    private String subjectName;

    /** 供应商编号 / 客户编号（模糊） */
    private String partnerCode;

    /** 供应商名称 / 客户名称（模糊） */
    private String partnerName;
}
