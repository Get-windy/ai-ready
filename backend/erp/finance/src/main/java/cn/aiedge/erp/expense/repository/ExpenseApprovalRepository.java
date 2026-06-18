package cn.aiedge.erp.expense.repository;

import cn.aiedge.erp.expense.model.ExpenseApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseApprovalRepository extends JpaRepository<ExpenseApproval, Long> {
    
    List<ExpenseApproval> findByApplicationId(String applicationId);
    
    List<ExpenseApproval> findByApplicationIdOrderByApprovalLevelAsc(String applicationId);
    
    List<ExpenseApproval> findByApproverId(String approverId);
}