package cn.aiedge.erp.finance.initial.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 期初试算平衡结果（「试算平衡」按钮）。
 *
 * <p>口径见 InitialFinanceServiceImpl#trialBalance：只统计「按科目」三类期初
 * （银行现金 / 固定资产 / 资产负债），借贷合计相等即平衡。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class TrialBalanceVO {

    /** 统计年度（未传时为 null，表示跨年度汇总） */
    private Integer periodYear;

    /** 借方合计 */
    private BigDecimal debitTotal;

    /** 贷方合计 */
    private BigDecimal creditTotal;

    /** 借贷差额（借方 - 贷方） */
    private BigDecimal difference;

    /** 是否平衡 */
    private boolean balanced;

    /** 参与试算的期初行数 */
    private long rowCount;

    /**
     * 存货对平检查（财务期初的存货类科目余额 vs 库存期初金额）。
     * 只报数、不阻断、不改写任何一侧 —— 口径见 {@link InventoryBalanceCheckVO}。
     */
    private InventoryBalanceCheckVO inventoryCheck;
}
