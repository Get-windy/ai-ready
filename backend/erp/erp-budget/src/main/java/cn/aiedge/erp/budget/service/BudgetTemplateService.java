package cn.aiedge.erp.budget.service;

import cn.aiedge.erp.budget.dto.BudgetTemplateDTO;
import cn.aiedge.erp.budget.dto.BudgetTemplateItemDTO;

import java.util.List;
import java.util.Map;

public interface BudgetTemplateService {

    BudgetTemplateDTO create(BudgetTemplateDTO dto);

    BudgetTemplateDTO update(Long id, BudgetTemplateDTO dto);

    void delete(Long id);

    BudgetTemplateDTO getById(Long id);

    Map<String, Object> page(String keyword, Integer fiscalYear, String status, int page, int size);

    BudgetTemplateDTO publish(Long id);

    List<BudgetTemplateDTO> listByFiscalYear(Integer fiscalYear);

    void batchDelete(List<Long> ids);

    List<BudgetTemplateDTO> exportList(String keyword, Integer fiscalYear, String status);
}
