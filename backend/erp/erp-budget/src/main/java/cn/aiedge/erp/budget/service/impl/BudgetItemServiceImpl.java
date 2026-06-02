package cn.aiedge.erp.budget.service.impl;

import cn.aiedge.erp.budget.dto.BudgetItemDTO;
import cn.aiedge.erp.budget.model.BudgetItem;
import cn.aiedge.erp.budget.repository.BudgetItemRepository;
import cn.aiedge.erp.budget.service.BudgetItemService;
import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetItemServiceImpl implements BudgetItemService {

    private final BudgetItemRepository budgetItemRepository;

    @Override
    public List<BudgetItemDTO> listByBudgetId(Long budgetId) {
        List<BudgetItem> items = budgetItemRepository.findByBudgetIdOrderBySortOrderAsc(budgetId);
        return items.stream().map(item -> {
            BudgetItemDTO dto = new BudgetItemDTO();
            BeanUtil.copyProperties(item, dto);
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BudgetItemDTO update(Long id, BudgetItemDTO dto) {
        BudgetItem entity = budgetItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("预算科目不存在: " + id));
        BeanUtil.copyProperties(dto, entity, "id", "budgetId");
        entity = budgetItemRepository.save(entity);

        BudgetItemDTO result = new BudgetItemDTO();
        BeanUtil.copyProperties(entity, result);
        return result;
    }

    @Override
    public BudgetItemDTO getById(Long id) {
        BudgetItem entity = budgetItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("预算科目不存在: " + id));
        BudgetItemDTO dto = new BudgetItemDTO();
        BeanUtil.copyProperties(entity, dto);
        return dto;
    }
}
