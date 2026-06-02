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
}
