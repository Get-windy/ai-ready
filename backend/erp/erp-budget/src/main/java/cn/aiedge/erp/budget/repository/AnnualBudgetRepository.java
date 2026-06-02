package cn.aiedge.erp.budget.repository;

import cn.aiedge.erp.budget.model.AnnualBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnualBudgetRepository extends JpaRepository<AnnualBudget, Long> {

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

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM AnnualBudget b WHERE b.deleted = false AND b.fiscalYear = :fiscalYear")
    java.math.BigDecimal sumTotalAmountByFiscalYear(@Param("fiscalYear") Integer fiscalYear);

    @Query("SELECT COALESCE(SUM(b.totalUsedAmount), 0) FROM AnnualBudget b WHERE b.deleted = false AND b.fiscalYear = :fiscalYear")
    java.math.BigDecimal sumTotalUsedAmountByFiscalYear(@Param("fiscalYear") Integer fiscalYear);
}
