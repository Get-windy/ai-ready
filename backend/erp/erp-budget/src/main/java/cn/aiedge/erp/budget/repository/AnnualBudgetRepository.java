package cn.aiedge.erp.budget.repository;

import cn.aiedge.erp.budget.model.AnnualBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnualBudgetRepository extends JpaRepository<AnnualBudget, Long>, JpaSpecificationExecutor<AnnualBudget> {

    List<AnnualBudget> findByFiscalYearAndDeletedFalse(Integer fiscalYear);

    List<AnnualBudget> findByDepartmentIdAndDeletedFalse(String departmentId);

    List<AnnualBudget> findByStatusAndDeletedFalse(String status);

    List<AnnualBudget> findByTemplateIdAndDeletedFalse(Long templateId);

    @Query("SELECT b FROM AnnualBudget b WHERE b.deleted = false AND " +
           "(:keyword IS NULL OR b.budgetNo LIKE %:keyword% OR b.departmentName LIKE %:keyword%) AND " +
           "(:fiscalYear IS NULL OR b.fiscalYear = :fiscalYear) AND " +
           "(:departmentId IS NULL OR b.departmentId = :departmentId) AND " +
           "(:status IS NULL OR b.status = :status) " +
           "ORDER BY b.createdAt DESC")
    List<AnnualBudget> search(@Param("keyword") String keyword,
                              @Param("fiscalYear") Integer fiscalYear,
                              @Param("departmentId") String departmentId,
                              @Param("status") String status);

    long countByStatusAndDeletedFalse(String status);

    /**
     * 单号号段：取同前缀下最大单号（用于生成下一单号）。
     * 注意：不过滤 deleted —— 单号唯一索引 uk_annual_budget_no 不含 deleted 条件，
     * 软删除的单号仍占用号段，过滤会导致重号。
     */
    java.util.Optional<AnnualBudget> findTopByBudgetNoStartingWithOrderByBudgetNoDesc(String prefix);

    /** 单号是否已存在（含软删除） */
    boolean existsByBudgetNo(String budgetNo);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM AnnualBudget b WHERE b.deleted = false AND b.fiscalYear = :fiscalYear")
    java.math.BigDecimal sumTotalAmountByFiscalYear(@Param("fiscalYear") Integer fiscalYear);

    @Query("SELECT COALESCE(SUM(b.totalUsedAmount), 0) FROM AnnualBudget b WHERE b.deleted = false AND b.fiscalYear = :fiscalYear")
    java.math.BigDecimal sumTotalUsedAmountByFiscalYear(@Param("fiscalYear") Integer fiscalYear);
}
