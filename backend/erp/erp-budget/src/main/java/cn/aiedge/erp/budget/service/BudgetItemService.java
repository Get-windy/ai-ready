package cn.aiedge.erp.budget.service;

import cn.aiedge.erp.budget.dto.BudgetItemDTO;

import java.util.List;

public interface BudgetItemService {

    List<BudgetItemDTO> listByBudgetId(Long budgetId);

    BudgetItemDTO update(Long id, BudgetItemDTO dto);

    BudgetItemDTO getById(Long id);
}
