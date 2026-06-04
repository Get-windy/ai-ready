package cn.aiedge.erp.budget.service.impl;

import cn.aiedge.erp.budget.dto.BudgetAdjustmentDTO;
import cn.aiedge.erp.budget.model.BudgetAdjustment;
import cn.aiedge.erp.budget.model.BudgetItem;
import cn.aiedge.erp.budget.repository.BudgetAdjustmentRepository;
import cn.aiedge.erp.budget.repository.BudgetItemRepository;
import cn.aiedge.erp.budget.service.BudgetAdjustmentService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetAdjustmentServiceImpl implements BudgetAdjustmentService {

    private final BudgetAdjustmentRepository budgetAdjustmentRepository;
    private final BudgetItemRepository budgetItemRepository;

    @Override
    @Transactional
    public BudgetAdjustmentDTO create(BudgetAdjustmentDTO dto) {
        BudgetAdjustment entity = new BudgetAdjustment();
        BeanUtil.copyProperties(dto, entity, "id");
        if (entity.getAdjustmentNo() == null) {
            entity.setAdjustmentNo("ADJ-" + IdUtil.fastSimpleUUID().substring(0, 8).toUpperCase());
        }
        if (entity.getStatus() == null) {
            entity.setStatus("draft");
        }
        if (entity.getApplyDate() == null) {
            entity.setApplyDate(LocalDate.now());
        }
        entity = budgetAdjustmentRepository.save(entity);

        BudgetAdjustmentDTO result = new BudgetAdjustmentDTO();
        BeanUtil.copyProperties(entity, result);
        return result;
    }

    @Override
    @Transactional
    public BudgetAdjustmentDTO update(Long id, BudgetAdjustmentDTO dto) {
        BudgetAdjustment entity = budgetAdjustmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("预算调整不存在: " + id));
        if (!"draft".equals(entity.getStatus())) {
            throw new RuntimeException("只有草稿状态的调整可以编辑");
        }
        BeanUtil.copyProperties(dto, entity, "id", "adjustmentNo", "status", "applicantId", "applicantName");
        entity = budgetAdjustmentRepository.save(entity);

        BudgetAdjustmentDTO result = new BudgetAdjustmentDTO();
        BeanUtil.copyProperties(entity, result);
        return result;
    }

    @Override
    public BudgetAdjustmentDTO getById(Long id) {
        BudgetAdjustment entity = budgetAdjustmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("预算调整不存在: " + id));
        BudgetAdjustmentDTO dto = new BudgetAdjustmentDTO();
        BeanUtil.copyProperties(entity, dto);

        if (entity.getSourceSubjectId() != null) {
            budgetItemRepository.findById(entity.getSourceSubjectId())
                    .ifPresent(item -> dto.setSourceSubjectName(item.getSubjectName()));
        }
        if (entity.getTargetSubjectId() != null) {
            budgetItemRepository.findById(entity.getTargetSubjectId())
                    .ifPresent(item -> dto.setTargetSubjectName(item.getSubjectName()));
        }

        return dto;
    }

    @Override
    public Map<String, Object> page(Long budgetId, String status, String adjustmentType, int page, int size) {
        List<BudgetAdjustment> allList = budgetAdjustmentRepository.search(budgetId, status, adjustmentType);

        int total = allList.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<BudgetAdjustment> pageList = allList.subList(fromIndex, toIndex);

        List<BudgetAdjustmentDTO> dtoList = pageList.stream().map(entity -> {
            BudgetAdjustmentDTO dto = new BudgetAdjustmentDTO();
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
    public BudgetAdjustmentDTO submit(Long id) {
        BudgetAdjustment entity = budgetAdjustmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("预算调整不存在: " + id));
        if (!"draft".equals(entity.getStatus())) {
            throw new RuntimeException("只有草稿状态的调整可以提交");
        }
        entity.setStatus("submitted");
        entity = budgetAdjustmentRepository.save(entity);

        BudgetAdjustmentDTO dto = new BudgetAdjustmentDTO();
        BeanUtil.copyProperties(entity, dto);
        return dto;
    }

    @Override
    @Transactional
    public BudgetAdjustmentDTO approve(Long id, String comment) {
        BudgetAdjustment entity = budgetAdjustmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("预算调整不存在: " + id));
        if (!"submitted".equals(entity.getStatus())) {
            throw new RuntimeException("只有已提交状态的调整可以审批通过");
        }
        entity.setStatus("approved");
        entity.setApprovalComment(comment);
        entity.setApprovalDate(LocalDate.now());
        entity = budgetAdjustmentRepository.save(entity);

        applyAdjustment(entity);

        BudgetAdjustmentDTO dto = new BudgetAdjustmentDTO();
        BeanUtil.copyProperties(entity, dto);
        return dto;
    }

    private void applyAdjustment(BudgetAdjustment entity) {
        if ("increase".equals(entity.getAdjustmentType()) && entity.getTargetSubjectId() != null) {
            budgetItemRepository.findById(entity.getTargetSubjectId()).ifPresent(item -> {
                item.setBudgetAmount(item.getBudgetAmount().add(entity.getAmount()));
                item.setRemainingAmount(item.getRemainingAmount().add(entity.getAmount()));
                budgetItemRepository.save(item);
            });
        } else if ("decrease".equals(entity.getAdjustmentType()) && entity.getSourceSubjectId() != null) {
            budgetItemRepository.findById(entity.getSourceSubjectId()).ifPresent(item -> {
                item.setBudgetAmount(item.getBudgetAmount().subtract(entity.getAmount()));
                item.setRemainingAmount(item.getRemainingAmount().subtract(entity.getAmount()));
                budgetItemRepository.save(item);
            });
        } else if ("transfer".equals(entity.getAdjustmentType()) &&
                   entity.getSourceSubjectId() != null && entity.getTargetSubjectId() != null) {
            budgetItemRepository.findById(entity.getSourceSubjectId()).ifPresent(source -> {
                source.setBudgetAmount(source.getBudgetAmount().subtract(entity.getAmount()));
                source.setRemainingAmount(source.getRemainingAmount().subtract(entity.getAmount()));
                budgetItemRepository.save(source);
            });
            budgetItemRepository.findById(entity.getTargetSubjectId()).ifPresent(target -> {
                target.setBudgetAmount(target.getBudgetAmount().add(entity.getAmount()));
                target.setRemainingAmount(target.getRemainingAmount().add(entity.getAmount()));
                budgetItemRepository.save(target);
            });
        }
    }

    @Override
    @Transactional
    public BudgetAdjustmentDTO reject(Long id, String comment) {
        BudgetAdjustment entity = budgetAdjustmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("预算调整不存在: " + id));
        if (!"submitted".equals(entity.getStatus())) {
            throw new RuntimeException("只有已提交状态的调整可以拒绝");
        }
        entity.setStatus("rejected");
        entity.setApprovalComment(comment);
        entity.setApprovalDate(LocalDate.now());
        entity = budgetAdjustmentRepository.save(entity);

        BudgetAdjustmentDTO dto = new BudgetAdjustmentDTO();
        BeanUtil.copyProperties(entity, dto);
        return dto;
    }

    @Override
    public List<BudgetAdjustmentDTO> exportList(Long budgetId, String status, String adjustmentType) {
        List<BudgetAdjustment> allList = budgetAdjustmentRepository.search(budgetId, status, adjustmentType);
        return allList.stream().map(entity -> {
            BudgetAdjustmentDTO dto = new BudgetAdjustmentDTO();
            BeanUtil.copyProperties(entity, dto);
            return dto;
        }).collect(Collectors.toList());
    }
}
