package cn.aiedge.erp.budget.repository;

import cn.aiedge.erp.budget.model.BudgetItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface BudgetItemRepository extends JpaRepository<BudgetItem, Long> {

    List<BudgetItem> findByBudgetIdOrderBySortOrderAsc(Long budgetId);

    List<BudgetItem> findByBudgetIdInAndDeletedFalse(Collection<Long> budgetIds);

    void deleteByBudgetId(Long budgetId);

    /**
     * 按「会计年度 + 部门 + 预算科目编码」查找可回写（已审批 / 执行中）的预算科目。
     *
     * <p>费用、支出单据记账后按费用科目编码匹配预算科目，用于回写《预算执行》。
     * 执行中优先于已审批，同状态按预算ID升序，保证匹配结果稳定可复现。</p>
     *
     * @param fiscalYear   会计年度
     * @param departmentId 部门ID，为空表示不限部门
     * @param subjectCode  预算科目编码
     */
    @Query("SELECT i FROM BudgetItem i, AnnualBudget b " +
           "WHERE i.budgetId = b.id AND i.deleted = false AND b.deleted = false " +
           "AND b.fiscalYear = :fiscalYear AND i.subjectCode = :subjectCode " +
           "AND b.status IN ('approved', 'executing') " +
           "AND (:departmentId IS NULL OR b.departmentId = :departmentId) " +
           "ORDER BY CASE WHEN b.status = 'executing' THEN 0 ELSE 1 END, b.id ASC")
    List<BudgetItem> findConsumableBySubject(@Param("fiscalYear") Integer fiscalYear,
                                             @Param("departmentId") String departmentId,
                                             @Param("subjectCode") String subjectCode);
}
