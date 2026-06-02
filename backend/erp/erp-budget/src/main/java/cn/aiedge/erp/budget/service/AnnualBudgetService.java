package cn.aiedge.erp.budget.service;

import cn.aiedge.erp.budget.dto.AnnualBudgetDTO;

import java.util.Map;

public interface AnnualBudgetService {

    AnnualBudgetDTO create(AnnualBudgetDTO dto);

    AnnualBudgetDTO update(Long id, AnnualBudgetDTO dto);

    void delete(Long id);

    AnnualBudgetDTO getById(Long id);

    Map<String, Object> page(String keyword, Integer fiscalYear, String departmentId, String status, int page, int size);

    AnnualBudgetDTO submit(Long id);

    AnnualBudgetDTO approve(Long id);

    AnnualBudgetDTO reject(Long id);

    AnnualBudgetDTO close(Long id);
}
