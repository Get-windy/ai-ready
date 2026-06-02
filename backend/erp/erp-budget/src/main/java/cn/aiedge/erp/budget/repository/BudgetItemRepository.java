package cn.aiedge.erp.budget.repository;

import cn.aiedge.erp.budget.model.BudgetItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetItemRepository extends JpaRepository<BudgetItem, Long> {

    List<BudgetItem> findByBudgetIdOrderBySortOrderAsc(Long budgetId);

    void deleteByBudgetId(Long budgetId);
}
