package cn.aiedge.erp.budget.repository;

import cn.aiedge.erp.budget.model.BudgetExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BudgetExecutionLogRepository extends JpaRepository<BudgetExecutionLog, Long> {

    List<BudgetExecutionLog> findByBudgetIdOrderByExecutionDateDesc(Long budgetId);

    List<BudgetExecutionLog> findByBudgetItemIdOrderByExecutionDateDesc(Long budgetItemId);

    List<BudgetExecutionLog> findByBudgetIdAndExecutionDateBetweenOrderByExecutionDateDesc(
            Long budgetId, LocalDate startDate, LocalDate endDate);

    List<BudgetExecutionLog> findBySourceTypeAndSourceId(String sourceType, Long sourceId);
}
