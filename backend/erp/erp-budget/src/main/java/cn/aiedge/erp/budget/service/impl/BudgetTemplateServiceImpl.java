package cn.aiedge.erp.budget.service.impl;

import cn.aiedge.erp.budget.dto.BudgetTemplateDTO;
import cn.aiedge.erp.budget.dto.BudgetTemplateItemDTO;
import cn.aiedge.erp.budget.model.BudgetTemplate;
import cn.aiedge.erp.budget.model.BudgetTemplateItem;
import cn.aiedge.erp.budget.repository.BudgetTemplateItemRepository;
import cn.aiedge.erp.budget.repository.BudgetTemplateRepository;
import cn.aiedge.erp.budget.service.BudgetTemplateService;
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
public class BudgetTemplateServiceImpl implements BudgetTemplateService {

    private final BudgetTemplateRepository budgetTemplateRepository;
    private final BudgetTemplateItemRepository budgetTemplateItemRepository;

    @Override
    @Transactional
    public BudgetTemplateDTO create(BudgetTemplateDTO dto) {
        BudgetTemplate entity = new BudgetTemplate();
        BeanUtil.copyProperties(dto, entity, "id", "items");
        if (entity.getTemplateCode() == null) {
            entity.setTemplateCode("TMPL-" + IdUtil.fastSimpleUUID().substring(0, 8).toUpperCase());
        }
        if (entity.getStatus() == null) {
            entity.setStatus("draft");
        }
        entity = budgetTemplateRepository.save(entity);

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (BudgetTemplateItemDTO itemDto : dto.getItems()) {
                BudgetTemplateItem item = new BudgetTemplateItem();
                BeanUtil.copyProperties(itemDto, item, "id");
                item.setTemplateId(entity.getId());
                budgetTemplateItemRepository.save(item);
            }
        }

        return getById(entity.getId());
    }

    @Override
    @Transactional
    public BudgetTemplateDTO update(Long id, BudgetTemplateDTO dto) {
        BudgetTemplate entity = budgetTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("预算模板不存在: " + id));

        BeanUtil.copyProperties(dto, entity, "id", "items", "templateCode", "status");
        entity = budgetTemplateRepository.save(entity);

        budgetTemplateItemRepository.deleteByTemplateId(id);
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (BudgetTemplateItemDTO itemDto : dto.getItems()) {
                BudgetTemplateItem item = new BudgetTemplateItem();
                BeanUtil.copyProperties(itemDto, item, "id");
                item.setTemplateId(id);
                budgetTemplateItemRepository.save(item);
            }
        }

        return getById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        BudgetTemplate entity = budgetTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("预算模板不存在: " + id));
        entity.markAsDeleted();
        budgetTemplateRepository.save(entity);
    }

    @Override
    public BudgetTemplateDTO getById(Long id) {
        BudgetTemplate entity = budgetTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("预算模板不存在: " + id));
        BudgetTemplateDTO dto = new BudgetTemplateDTO();
        BeanUtil.copyProperties(entity, dto);

        List<BudgetTemplateItem> items = budgetTemplateItemRepository.findByTemplateIdOrderBySortOrderAsc(id);
        dto.setItems(items.stream().map(item -> {
            BudgetTemplateItemDTO itemDto = new BudgetTemplateItemDTO();
            BeanUtil.copyProperties(item, itemDto);
            return itemDto;
        }).collect(Collectors.toList()));

        return dto;
    }

    @Override
    public Map<String, Object> page(String keyword, Integer fiscalYear, String status, int page, int size) {
        List<BudgetTemplate> allList = budgetTemplateRepository.search(keyword, fiscalYear, status);

        int total = allList.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<BudgetTemplate> pageList = allList.subList(fromIndex, toIndex);

        List<BudgetTemplateDTO> dtoList = pageList.stream().map(entity -> {
            BudgetTemplateDTO dto = new BudgetTemplateDTO();
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
    public BudgetTemplateDTO publish(Long id) {
        BudgetTemplate entity = budgetTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("预算模板不存在: " + id));
        entity.setStatus("published");
        entity = budgetTemplateRepository.save(entity);

        BudgetTemplateDTO dto = new BudgetTemplateDTO();
        BeanUtil.copyProperties(entity, dto);
        return dto;
    }

    @Override
    public List<BudgetTemplateDTO> listByFiscalYear(Integer fiscalYear) {
        List<BudgetTemplate> list = budgetTemplateRepository.findByFiscalYearAndDeletedFalseOrderByCreatedAtDesc(fiscalYear);
        return list.stream().map(entity -> {
            BudgetTemplateDTO dto = new BudgetTemplateDTO();
            BeanUtil.copyProperties(entity, dto);
            return dto;
        }).collect(Collectors.toList());
    }
}
