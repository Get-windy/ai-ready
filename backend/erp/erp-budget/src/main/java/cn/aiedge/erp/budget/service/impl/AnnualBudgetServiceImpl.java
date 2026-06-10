package cn.aiedge.erp.budget.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.budget.dto.AnnualBudgetDTO;
import cn.aiedge.erp.budget.dto.BudgetItemDTO;
import cn.aiedge.erp.budget.model.AnnualBudget;
import cn.aiedge.erp.budget.model.BudgetItem;
import cn.aiedge.erp.budget.model.BudgetTemplate;
import cn.aiedge.erp.budget.model.BudgetTemplateItem;
import cn.aiedge.erp.budget.repository.AnnualBudgetRepository;
import cn.aiedge.erp.budget.repository.BudgetItemRepository;
import cn.aiedge.erp.budget.repository.BudgetTemplateItemRepository;
import cn.aiedge.erp.budget.repository.BudgetTemplateRepository;
import cn.aiedge.erp.budget.service.AnnualBudgetService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnualBudgetServiceImpl implements AnnualBudgetService {

    private final AnnualBudgetRepository annualBudgetRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final BudgetTemplateRepository budgetTemplateRepository;
    private final BudgetTemplateItemRepository budgetTemplateItemRepository;

    @Override
    @Transactional
    public AnnualBudgetDTO create(AnnualBudgetDTO dto) {
        AnnualBudget entity = new AnnualBudget();
        BeanUtil.copyProperties(dto, entity, "id", "items");
        if (entity.getBudgetNo() == null) {
            entity.setBudgetNo("BUD-" + IdUtil.fastSimpleUUID().substring(0, 8).toUpperCase());
        }
        if (entity.getStatus() == null) {
            entity.setStatus("draft");
        }
        entity.setTotalUsedAmount(BigDecimal.ZERO);
        entity.setTotalRemainingAmount(entity.getTotalAmount() != null ? entity.getTotalAmount() : BigDecimal.ZERO);
        entity.setTotalApprovedAmount(BigDecimal.ZERO);
        entity.setExecutionRate(BigDecimal.ZERO);
        entity = annualBudgetRepository.save(entity);

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (BudgetItemDTO itemDto : dto.getItems()) {
                BudgetItem item = new BudgetItem();
                BeanUtil.copyProperties(itemDto, item, "id");
                item.setBudgetId(entity.getId());
                item.setUsedAmount(BigDecimal.ZERO);
                item.setRemainingAmount(item.getBudgetAmount() != null ? item.getBudgetAmount() : BigDecimal.ZERO);
                item.setFrozenAmount(BigDecimal.ZERO);
                item.setExecutionRate(BigDecimal.ZERO);
                budgetItemRepository.save(item);
            }
        } else if (dto.getTemplateId() != null) {
            copyItemsFromTemplate(dto.getTemplateId(), entity.getId());
        }

        return getById(entity.getId());
    }

    private void copyItemsFromTemplate(Long templateId, Long budgetId) {
        List<BudgetTemplateItem> templateItems = budgetTemplateItemRepository.findByTemplateIdOrderBySortOrderAsc(templateId);
        for (BudgetTemplateItem templateItem : templateItems) {
            BudgetItem item = new BudgetItem();
            BeanUtil.copyProperties(templateItem, item, "id", "templateId");
            item.setBudgetId(budgetId);
            item.setUsedAmount(BigDecimal.ZERO);
            item.setRemainingAmount(item.getBudgetAmount() != null ? item.getBudgetAmount() : BigDecimal.ZERO);
            item.setFrozenAmount(BigDecimal.ZERO);
            item.setExecutionRate(BigDecimal.ZERO);
            budgetItemRepository.save(item);
        }
    }

    @Override
    @Transactional
    public AnnualBudgetDTO update(Long id, AnnualBudgetDTO dto) {
        AnnualBudget entity = annualBudgetRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("年度预算不存在: " + id));

        if (!"draft".equals(entity.getStatus())) {
            throw BusinessException.badRequest("只有草稿状态的预算可以编辑");
        }

        BeanUtil.copyProperties(dto, entity, "id", "items", "budgetNo", "status",
                "totalUsedAmount", "totalRemainingAmount", "totalApprovedAmount", "executionRate");
        entity = annualBudgetRepository.save(entity);

        budgetItemRepository.deleteByBudgetId(id);
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (BudgetItemDTO itemDto : dto.getItems()) {
                BudgetItem item = new BudgetItem();
                BeanUtil.copyProperties(itemDto, item, "id");
                item.setBudgetId(id);
                item.setUsedAmount(BigDecimal.ZERO);
                item.setRemainingAmount(item.getBudgetAmount() != null ? item.getBudgetAmount() : BigDecimal.ZERO);
                item.setFrozenAmount(BigDecimal.ZERO);
                item.setExecutionRate(BigDecimal.ZERO);
                budgetItemRepository.save(item);
            }
        }

        recalculateBudget(entity);

        return getById(id);
    }

    private void recalculateBudget(AnnualBudget entity) {
        List<BudgetItem> items = budgetItemRepository.findByBudgetIdOrderBySortOrderAsc(entity.getId());
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalUsed = BigDecimal.ZERO;
        BigDecimal totalRemaining = BigDecimal.ZERO;

        for (BudgetItem item : items) {
            totalAmount = totalAmount.add(item.getBudgetAmount() != null ? item.getBudgetAmount() : BigDecimal.ZERO);
            totalUsed = totalUsed.add(item.getUsedAmount() != null ? item.getUsedAmount() : BigDecimal.ZERO);
            totalRemaining = totalRemaining.add(item.getRemainingAmount() != null ? item.getRemainingAmount() : BigDecimal.ZERO);
        }

        entity.setTotalAmount(totalAmount);
        entity.setTotalUsedAmount(totalUsed);
        entity.setTotalRemainingAmount(totalRemaining);
        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            entity.setExecutionRate(totalUsed.multiply(BigDecimal.valueOf(100))
                    .divide(totalAmount, 2, java.math.RoundingMode.HALF_UP));
        } else {
            entity.setExecutionRate(BigDecimal.ZERO);
        }
        annualBudgetRepository.save(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AnnualBudget entity = annualBudgetRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("年度预算不存在: " + id));
        entity.markAsDeleted();
        annualBudgetRepository.save(entity);
    }

    @Override
    public AnnualBudgetDTO getById(Long id) {
        AnnualBudget entity = annualBudgetRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("年度预算不存在: " + id));
        AnnualBudgetDTO dto = new AnnualBudgetDTO();
        BeanUtil.copyProperties(entity, dto);

        List<BudgetItem> items = budgetItemRepository.findByBudgetIdOrderBySortOrderAsc(id);
        dto.setItems(items.stream().map(item -> {
            BudgetItemDTO itemDto = new BudgetItemDTO();
            BeanUtil.copyProperties(item, itemDto);
            return itemDto;
        }).collect(Collectors.toList()));

        return dto;
    }

    @Override
    public Map<String, Object> page(String keyword, Integer fiscalYear, String departmentId, String status, int page, int size) {
        List<AnnualBudget> allList = annualBudgetRepository.search(keyword, fiscalYear, departmentId, status);

        int total = allList.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<AnnualBudget> pageList = allList.subList(fromIndex, toIndex);

        List<AnnualBudgetDTO> dtoList = pageList.stream().map(entity -> {
            AnnualBudgetDTO dto = new AnnualBudgetDTO();
            BeanUtil.copyProperties(entity, dto);
            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", dtoList);
        result.put("total", (long) total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    @Transactional
    public AnnualBudgetDTO submit(Long id) {
        AnnualBudget entity = annualBudgetRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("年度预算不存在: " + id));
        if (!"draft".equals(entity.getStatus())) {
            throw BusinessException.badRequest("只有草稿状态的预算可以提交");
        }
        entity.setStatus("submitted");
        entity = annualBudgetRepository.save(entity);
        return getById(entity.getId());
    }

    @Override
    @Transactional
    public AnnualBudgetDTO approve(Long id) {
        AnnualBudget entity = annualBudgetRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("年度预算不存在: " + id));
        if (!"submitted".equals(entity.getStatus())) {
            throw BusinessException.badRequest("只有已提交状态的预算可以审批通过");
        }
        entity.setStatus("approved");
        entity.setTotalApprovedAmount(entity.getTotalAmount());
        annualBudgetRepository.save(entity);
        return getById(entity.getId());
    }

    @Override
    @Transactional
    public AnnualBudgetDTO reject(Long id) {
        AnnualBudget entity = annualBudgetRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("年度预算不存在: " + id));
        if (!"submitted".equals(entity.getStatus())) {
            throw BusinessException.badRequest("只有已提交状态的预算可以拒绝");
        }
        entity.setStatus("rejected");
        entity = annualBudgetRepository.save(entity);
        return getById(entity.getId());
    }

    @Override
    @Transactional
    public AnnualBudgetDTO startExec(Long id) {
        AnnualBudget entity = annualBudgetRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("年度预算不存在: " + id));
        if (!"approved".equals(entity.getStatus())) {
            throw BusinessException.badRequest("只有已审批状态的预算可以开始执行");
        }
        entity.setStatus("executing");
        entity = annualBudgetRepository.save(entity);
        return getById(entity.getId());
    }

    @Override
    @Transactional
    public AnnualBudgetDTO close(Long id) {
        AnnualBudget entity = annualBudgetRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("年度预算不存在: " + id));
        if (!"executing".equals(entity.getStatus())) {
            throw BusinessException.badRequest("只有执行中的预算可以关闭");
        }
        entity.setStatus("closed");
        entity = annualBudgetRepository.save(entity);
        return getById(entity.getId());
    }

    @Override
    public List<AnnualBudgetDTO> exportList(String keyword, Integer fiscalYear, String departmentId, String status) {
        List<AnnualBudget> allList = annualBudgetRepository.search(keyword, fiscalYear, departmentId, status);
        return allList.stream().map(entity -> {
            AnnualBudgetDTO dto = new AnnualBudgetDTO();
            BeanUtil.copyProperties(entity, dto);
            return dto;
        }).collect(Collectors.toList());
    }
}
