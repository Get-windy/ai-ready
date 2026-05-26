package cn.aiedge.erp.expense.repository;

import cn.aiedge.erp.expense.model.ExpenseApplication;
import cn.aiedge.erp.expense.model.enumeration.ExpenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 费用申请Repository接口
 */
@Repository
public interface ExpenseApplicationRepository extends JpaRepository<ExpenseApplication, Long>, 
                                                     JpaSpecificationExecutor<ExpenseApplication> {
    
    /**
     * 根据申请编号查询
     */
    Optional<ExpenseApplication> findByApplicationCode(String applicationCode);
    
    /**
     * 根据申请人ID查询
     */
    List<ExpenseApplication> findByApplicantId(String applicantId);
    
    /**
     * 根据申请人ID和状态查询
     */
    List<ExpenseApplication> findByApplicantIdAndStatus(String applicantId, ExpenseStatus status);
    
    /**
     * 根据申请人ID和状态分页查询
     */
    Page<ExpenseApplication> findByApplicantIdAndStatus(String applicantId, ExpenseStatus status, Pageable pageable);
    
    /**
     * 根据部门ID查询
     */
    List<ExpenseApplication> findByDepartmentId(String departmentId);
    
    /**
     * 根据部门ID和状态查询
     */
    List<ExpenseApplication> findByDepartmentIdAndStatus(String departmentId, ExpenseStatus status);
    
    /**
     * 根据状态查询
     */
    List<ExpenseApplication> findByStatus(ExpenseStatus status);
    
    /**
     * 根据状态分页查询
     */
    Page<ExpenseApplication> findByStatus(ExpenseStatus status, Pageable pageable);
    
    /**
     * 根据申请日期范围查询
     */
    List<ExpenseApplication> findByApplyDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * 根据申请日期范围和状态查询
     */
    List<ExpenseApplication> findByApplyDateBetweenAndStatus(LocalDate startDate, LocalDate endDate, ExpenseStatus status);
    
    /**
     * 根据当前审批人ID查询
     */
    List<ExpenseApplication> findByCurrentApproverId(String currentApproverId);
    
    /**
     * 根据当前审批人ID和状态查询
     */
    List<ExpenseApplication> findByCurrentApproverIdAndStatus(String currentApproverId, ExpenseStatus status);
    
    /**
     * 根据费用类型查询
     */
    List<ExpenseApplication> findByExpenseType(String expenseType);
    
    /**
     * 根据费用类型和状态查询
     */
    List<ExpenseApplication> findByExpenseTypeAndStatus(String expenseType, ExpenseStatus status);
    
    /**
     * 根据预算科目ID查询
     */
    List<ExpenseApplication> findByBudgetSubjectId(String budgetSubjectId);
    
    /**
     * 统计申请人总费用
     */
    @Query("SELECT COALESCE(SUM(e.totalAmount), 0) FROM ExpenseApplication e WHERE e.applicantId = :applicantId AND e.status IN :statuses")
    BigDecimal sumTotalAmountByApplicantIdAndStatusIn(@Param("applicantId") String applicantId, 
                                                     @Param("statuses") List<ExpenseStatus> statuses);
    
    /**
     * 统计部门总费用
     */
    @Query("SELECT COALESCE(SUM(e.totalAmount), 0) FROM ExpenseApplication e WHERE e.departmentId = :departmentId AND e.status IN :statuses")
    BigDecimal sumTotalAmountByDepartmentIdAndStatusIn(@Param("departmentId") String departmentId, 
                                                      @Param("statuses") List<ExpenseStatus> statuses);
    
    /**
     * 统计预算科目总费用
     */
    @Query("SELECT COALESCE(SUM(e.totalAmount), 0) FROM ExpenseApplication e WHERE e.budgetSubjectId = :budgetSubjectId AND e.status IN :statuses")
    BigDecimal sumTotalAmountByBudgetSubjectIdAndStatusIn(@Param("budgetSubjectId") String budgetSubjectId, 
                                                         @Param("statuses") List<ExpenseStatus> statuses);
    
    /**
     * 统计费用类型总费用
     */
    @Query("SELECT COALESCE(SUM(e.totalAmount), 0) FROM ExpenseApplication e WHERE e.expenseType = :expenseType AND e.status IN :statuses")
    BigDecimal sumTotalAmountByExpenseTypeAndStatusIn(@Param("expenseType") String expenseType, 
                                                     @Param("statuses") List<ExpenseStatus> statuses);
    
    /**
     * 根据多个状态查询
     */
    List<ExpenseApplication> findByStatusIn(List<ExpenseStatus> statuses);
    
    /**
     * 根据多个状态分页查询
     */
    Page<ExpenseApplication> findByStatusIn(List<ExpenseStatus> statuses, Pageable pageable);
    
    /**
     * 根据申请日期范围、状态和申请人ID查询
     */
    List<ExpenseApplication> findByApplyDateBetweenAndStatusAndApplicantId(LocalDate startDate, 
                                                                          LocalDate endDate, 
                                                                          ExpenseStatus status, 
                                                                          String applicantId);
    
    /**
     * 检查申请编号是否存在
     */
    boolean existsByApplicationCode(String applicationCode);
    
    /**
     * 根据工作流实例ID查询
     */
    Optional<ExpenseApplication> findByProcessInstanceId(String processInstanceId);
    
    /**
     * 根据工作流任务ID查询
     */
    Optional<ExpenseApplication> findByTaskId(String taskId);
}