package cn.aiedge.erp.budget.service;

import cn.aiedge.erp.budget.dto.BudgetAdjustmentDTO;

import java.util.List;
import java.util.Map;

public interface BudgetAdjustmentService {

    BudgetAdjustmentDTO create(BudgetAdjustmentDTO dto);

    BudgetAdjustmentDTO update(Long id, BudgetAdjustmentDTO dto);

    BudgetAdjustmentDTO getById(Long id);

    Map<String, Object> page(Long budgetId, String status, String adjustmentType, int page, int size);

    BudgetAdjustmentDTO submit(Long id);

    BudgetAdjustmentDTO approve(Long id, String comment);

    BudgetAdjustmentDTO reject(Long id, String comment);

    List<BudgetAdjustmentDTO> exportList(Long budgetId, String status, String adjustmentType);

    void delete(Long id);
}
