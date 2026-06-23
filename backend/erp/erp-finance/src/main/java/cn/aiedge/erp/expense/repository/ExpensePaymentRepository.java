package cn.aiedge.erp.expense.repository;

import cn.aiedge.erp.expense.model.ExpensePayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpensePaymentRepository extends JpaRepository<ExpensePayment, Long>,
                                                   JpaSpecificationExecutor<ExpensePayment> {

    List<ExpensePayment> findByApplicationId(Long applicationId);

    List<ExpensePayment> findByPayerId(String payerId);

    Page<ExpensePayment> findByPaymentStatus(String paymentStatus, Pageable pageable);
}
