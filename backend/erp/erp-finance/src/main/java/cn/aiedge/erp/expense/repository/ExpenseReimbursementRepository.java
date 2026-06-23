package cn.aiedge.erp.expense.repository;

import cn.aiedge.erp.expense.model.ExpenseReimbursement;
import cn.aiedge.erp.expense.model.enumeration.ExpenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseReimbursementRepository extends JpaRepository<ExpenseReimbursement, Long>,
                                                        JpaSpecificationExecutor<ExpenseReimbursement> {

    List<ExpenseReimbursement> findByApplicationId(Long applicationId);

    List<ExpenseReimbursement> findByApplicantId(String applicantId);

    Page<ExpenseReimbursement> findByStatus(ExpenseStatus status, Pageable pageable);

    List<ExpenseReimbursement> findByDepartmentId(String departmentId);
}
