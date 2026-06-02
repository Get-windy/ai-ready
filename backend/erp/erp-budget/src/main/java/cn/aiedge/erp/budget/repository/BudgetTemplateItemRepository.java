package cn.aiedge.erp.budget.repository;

import cn.aiedge.erp.budget.model.BudgetTemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetTemplateItemRepository extends JpaRepository<BudgetTemplateItem, Long> {

    List<BudgetTemplateItem> findByTemplateIdOrderBySortOrderAsc(Long templateId);

    void deleteByTemplateId(Long templateId);
}
