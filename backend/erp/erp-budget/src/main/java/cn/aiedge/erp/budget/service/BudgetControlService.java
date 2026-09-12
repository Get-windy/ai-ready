package cn.aiedge.erp.budget.service;

import java.util.Map;

public interface BudgetControlService {

    Map<String, Object> checkAvailability(Long budgetId, Long budgetItemId, java.math.BigDecimal amount);

    Map<String, Object> freezeAmount(Long budgetId, Long budgetItemId, java.math.BigDecimal amount,
                                     String sourceType, String sourceNo, Long sourceId);

    Map<String, Object> releaseFrozenAmount(Long budgetId, Long budgetItemId, java.math.BigDecimal amount,
                                            String sourceType, String sourceNo, Long sourceId);

    Map<String, Object> consumeBudget(Long budgetId, Long budgetItemId, java.math.BigDecimal amount,
                                      String sourceType, String sourceNo, Long sourceId, String description);

    /**
     * 记录预算消耗（可容忍超支）。
     *
     * <p>单据已经记账的场景（如费用单记账回写）不允许因预算不足中断记账，
     * 此时以 {@code allowOverBudget=true} 调用：超支部分照常计入已执行，
     * 剩余额可为负，并在返回值中给出 {@code overBudget=true} 供《预算执行》超支预警使用。</p>
     *
     * @param allowOverBudget 是否允许超支（true=超支不抛异常，转预警）
     * @return 消耗结果，含 usedAmount/remainingAmount/executionRate/overBudget
     */
    Map<String, Object> consumeBudget(Long budgetId, Long budgetItemId, java.math.BigDecimal amount,
                                      String sourceType, String sourceNo, Long sourceId, String description,
                                      boolean allowOverBudget);
}
