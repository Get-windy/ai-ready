package cn.aiedge.erp.budget.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 预算执行统计DTO
 *
 * <p>口径：剩余 = 预算 − 已执行 − 冻结；执行率 = 已执行 / 预算 × 100%。
 * 金额均按预算科目明细实时聚合，非取汇总列。</p>
 */
@Data
public class BudgetStatisticsDTO {
    private BigDecimal totalBudgetAmount;
    private BigDecimal totalUsedAmount;
    private BigDecimal totalRemainingAmount;
    private BigDecimal totalFrozenAmount;
    private BigDecimal executionRate;
    private Integer totalBudgetCount;
    private Integer executingCount;
    private Integer closedCount;
    private Integer draftCount;
    private BigDecimal increaseAmount;
    private BigDecimal decreaseAmount;

    // ═══ 金标准执行跟踪 ═══
    /** 已审批 / 执行中 / 已关闭（已转入执行的预算单数） */
    private Integer approvedCount;
    /** 预算科目数 */
    private Integer totalItemCount;
    /** 超支科目数（执行进度 > 100%） */
    private Integer overBudgetCount;
    /** 预警科目数（执行进度 ≥ 90% 且未超支） */
    private Integer warningCount;
    /** 超支金额合计 */
    private BigDecimal overBudgetAmount;
}
