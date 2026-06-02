package cn.aiedge.erp.budget.repository;

import cn.aiedge.erp.budget.model.BudgetAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetAdjustmentRepository extends JpaRepository<BudgetAdjustment, Long> {

    List<BudgetAdjustment> findByBudgetIdOrderByCreatedAtDesc(Long budgetId);

    List<BudgetAdjustment> findByStatusAndDeletedFalse(String status);

    @Query("SELECT a FROM BudgetAdjustment a WHERE a.deleted = false AND " +
           "(:budgetId IS NULL OR a.budgetId = :budgetId) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:adjustmentType IS NULL OR a.adjustmentType = :adjustmentType) " +
           "ORDER BY a.createdAt DESC")
    List<BudgetAdjustment> search(@Param("budgetId") Long budgetId,
                                  @Param("status") String status,
                                  @Param("adjustmentType") String adjustmentType);
}
