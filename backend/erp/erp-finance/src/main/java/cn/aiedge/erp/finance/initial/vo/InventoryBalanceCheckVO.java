package cn.aiedge.erp.finance.initial.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 「存货对平检查」结果 —— 财务期初的**存货类科目**期初余额 与 库存期初金额（Σ 数量×单价）的比对。
 *
 * <p><b>口径来源</b>：开发文档 §5.4「『存货科目期初』应与库存期初金额**对平**」+
 * §10.1 第 32 项验收「把『存货类科目期初合计』与《库存期初》的 Σ(quantity×unitPrice) 比对 →
 * 断言能报出差异」。</p>
 *
 * <p><b>存货类科目的识别</b>：本系统既有的资产负债表口径里，资产侧固定项目按**科目编号前缀**
 * 匹配（{@code FinancialReportServiceImpl.ASSET_LINE_DEFS} 中 {@code {"140", "库存商品"}}，
 * 覆盖 1401 材料采购 / 1403 原材料 / 1405 库存商品等全部 14xx 存货类科目）
 * → 本检查**沿用同一前缀 "140"**，不新造映射表。</p>
 *
 * <p><b>只报数不改数</b>：本检查**不自动改写任何一侧**（不补库存、不调财务），
 * 也不阻断保存（是否强制对平属产品裁定）。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class InventoryBalanceCheckVO {

    /** 存货类科目前缀（沿用资产负债表口径：140） */
    private String subjectCodePrefix;

    /** 财务侧：存货类科目期初余额合计（erp_initial_finance_subject，按年度过滤） */
    private BigDecimal financeAmount;

    /** 库存侧：库存期初金额合计 Σ(quantity × unit_price)（erp_stock.is_initial = 1） */
    private BigDecimal stockAmount;

    /** 差额 = 财务侧 - 库存侧 */
    private BigDecimal difference;

    /** 是否对平（差额绝对值 < 0.01，与资产负债表的平衡容差同口径） */
    private boolean matched;

    /** 财务侧参与合计的行数 */
    private long financeRowCount;

    /** 库存侧参与合计的行数（0 表示还没录库存期初） */
    private long stockRowCount;

    /** 结论说明（前端提示区直接展示） */
    private String note;
}
