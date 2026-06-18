package cn.aiedge.erp.expense.service;

import cn.aiedge.erp.expense.dto.ExpenseApplicationDTO;
import cn.aiedge.erp.expense.dto.ExpenseRequest;
import cn.aiedge.erp.expense.model.enumeration.ExpenseStatus;
import cn.aiedge.erp.expense.model.enumeration.ExpenseType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ExpenseService {
    
    ExpenseApplicationDTO applyExpense(ExpenseRequest request);
    
    ExpenseApplicationDTO getExpenseDetail(Long id);
    
    ExpenseApplicationDTO updateExpense(Long id, ExpenseRequest request);
    
    void deleteExpense(Long id);
    
    ExpenseApplicationDTO submitForApproval(Long id);
    
    ExpenseApplicationDTO approveExpense(Long id, String comment);
    
    ExpenseApplicationDTO rejectExpense(Long id, String reason);
    
    List<ExpenseApplicationDTO> getExpenseList(String applicantId, String departmentId, 
                                                ExpenseStatus status, LocalDate startDate, 
                                                LocalDate endDate, int page, int size);
    
    Map<String, Object> getExpenseStatistics(LocalDate startDate, LocalDate endDate, 
                                              String departmentId, ExpenseType expenseType);
}