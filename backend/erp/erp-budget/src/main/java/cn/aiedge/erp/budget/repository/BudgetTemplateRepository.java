package cn.aiedge.erp.budget.repository;

import cn.aiedge.erp.budget.model.BudgetTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetTemplateRepository extends JpaRepository<BudgetTemplate, Long> {

    List<BudgetTemplate> findByFiscalYearAndDeletedFalseOrderByCreatedAtDesc(Integer fiscalYear);

    List<BudgetTemplate> findByStatusAndDeletedFalse(String status);

    @Query("SELECT t FROM BudgetTemplate t WHERE t.deleted = false AND " +
           "(:keyword IS NULL OR t.templateName LIKE %:keyword% OR t.templateCode LIKE %:keyword%) AND " +
           "(:fiscalYear IS NULL OR t.fiscalYear = :fiscalYear) AND " +
           "(:status IS NULL OR t.status = :status) " +
           "ORDER BY t.createdAt DESC")
    List<BudgetTemplate> search(@Param("keyword") String keyword,
                                @Param("fiscalYear") Integer fiscalYear,
                                @Param("status") String status);

    long countByStatusAndDeletedFalse(String status);
}
