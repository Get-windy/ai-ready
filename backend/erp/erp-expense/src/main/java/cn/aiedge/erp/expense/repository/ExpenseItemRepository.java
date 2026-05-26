package cn.aiedge.erp.expense.repository;

import cn.aiedge.erp.expense.model.ExpenseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseItemRepository extends JpaRepository<ExpenseItem, Long> {
    
    List<ExpenseItem> findByExpenseApplicationId(Long expenseApplicationId);
    
    void deleteByExpenseApplicationId(Long expenseApplicationId);
}