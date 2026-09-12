package cn.aiedge.erp.budget.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.budget.dto.AnnualBudgetDTO;
import cn.aiedge.erp.budget.dto.AnnualBudgetQuery;
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
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        if (entity.getBudgetNo() == null || entity.getBudgetNo().isBlank()
                || annualBudgetRepository.existsByBudgetNo(entity.getBudgetNo())) {
            String generated = generateDocNo();
            int guard = 0;
            while (annualBudgetRepository.existsByBudgetNo(generated) && guard++ < 5) {
                generated = generateDocNo();
            }
            entity.setBudgetNo(generated);
        }
        if (entity.getStatus() == null) {
            entity.setStatus("draft");
        }
        entity.setTotalUsedAmount(BigDecimal.ZERO);
        entity.setTotalRemainingAmount(entity.getTotalAmount() != null ? entity.getTotalAmount() : BigDecimal.ZERO);
        entity.setTotalApprovedAmount(BigDecimal.ZERO);
        entity.setTotalFrozenAmount(BigDecimal.ZERO);
        entity.setExecutionRate(BigDecimal.ZERO);
        if (entity.getBudgetDate() == null) {
            entity.setBudgetDate(java.time.LocalDate.now());
        }
        if (entity.getPrintCount() == null) {
            entity.setPrintCount(0);
        }
        entity = annualBudgetRepository.save(entity);

        saveItems(entity.getId(), dto.getItems());
        if ((dto.getItems() == null || dto.getItems().isEmpty()) && dto.getTemplateId() != null) {
            copyItemsFromTemplate(dto.getTemplateId(), entity.getId());
        }

        return getById(entity.getId());
    }

    /** 保存预算科目明细（行号重排；已执行/冻结/剩余按预算金额初始化） */
    private void saveItems(Long budgetId, List<BudgetItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        int lineNo = 1;
        for (BudgetItemDTO itemDto : items) {
            BudgetItem item = new BudgetItem();
            BeanUtil.copyProperties(itemDto, item, "id");
            item.setBudgetId(budgetId);
            item.setLineNo(lineNo);
            item.setSortOrder(lineNo);
            item.setUsedAmount(BigDecimal.ZERO);
            item.setRemainingAmount(item.getBudgetAmount() != null ? item.getBudgetAmount() : BigDecimal.ZERO);
            item.setFrozenAmount(BigDecimal.ZERO);
            item.setExecutionRate(BigDecimal.ZERO);
            budgetItemRepository.save(item);
            lineNo++;
        }
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

        if (!"draft".equals(entity.getStatus()) && !"rejected".equals(entity.getStatus())) {
            throw BusinessException.badRequest("只有草稿或已驳回状态的预算可以编辑");
        }

        BeanUtil.copyProperties(dto, entity, "id", "items", "budgetNo", "status",
                "totalUsedAmount", "totalRemainingAmount", "totalApprovedAmount", "executionRate");
        entity = annualBudgetRepository.save(entity);

        budgetItemRepository.deleteByBudgetId(id);
        saveItems(id, dto.getItems());

        recalculateBudget(entity);

        return getById(id);
    }

    private void recalculateBudget(AnnualBudget entity) {
        List<BudgetItem> items = budgetItemRepository.findByBudgetIdOrderBySortOrderAsc(entity.getId());
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalUsed = BigDecimal.ZERO;
        BigDecimal totalRemaining = BigDecimal.ZERO;
        BigDecimal totalFrozen = BigDecimal.ZERO;

        for (BudgetItem item : items) {
            totalAmount = totalAmount.add(item.getBudgetAmount() != null ? item.getBudgetAmount() : BigDecimal.ZERO);
            totalUsed = totalUsed.add(item.getUsedAmount() != null ? item.getUsedAmount() : BigDecimal.ZERO);
            totalFrozen = totalFrozen.add(item.getFrozenAmount() != null ? item.getFrozenAmount() : BigDecimal.ZERO);
            // 剩余一律以明细的 remaining 为准（= 预算 − 已执行 − 冻结）
            totalRemaining = totalRemaining.add(item.getRemainingAmount() != null ? item.getRemainingAmount() : BigDecimal.ZERO);
        }

        entity.setTotalAmount(totalAmount);
        entity.setTotalUsedAmount(totalUsed);
        entity.setTotalFrozenAmount(totalFrozen);
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
    public void recalculateSummary(Long budgetId) {
        AnnualBudget entity = annualBudgetRepository.findById(budgetId).orElse(null);
        if (entity == null) {
            return;
        }
        recalculateBudget(entity);
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
    @Transactional
    public AnnualBudgetDTO submit(Long id) {
        AnnualBudget entity = annualBudgetRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("年度预算不存在: " + id));
        if (!"draft".equals(entity.getStatus()) && !"rejected".equals(entity.getStatus())) {
            throw BusinessException.badRequest("只有草稿或已驳回状态的预算可以提交");
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
        entity.setAuditTime(LocalDateTime.now());
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

    // ═══════════════════════════════════════════════════════════
    // 金标准编制能力
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public AnnualBudgetDTO save(AnnualBudgetDTO dto) {
        if (dto.getId() != null && annualBudgetRepository.existsById(dto.getId())) {
            return update(dto.getId(), dto);
        }
        return create(dto);
    }

    @Override
    public String generateDocNo() {
        String prefix = "YSD";
        String head = prefix + "-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Optional<AnnualBudget> last = annualBudgetRepository.findTopByBudgetNoStartingWithOrderByBudgetNoDesc(head);
        int seq = 1;
        if (last.isPresent() && last.get().getBudgetNo() != null) {
            String lastNo = last.get().getBudgetNo();
            String tail = lastNo.substring(lastNo.lastIndexOf('-') + 1);
            try {
                seq = Integer.parseInt(tail) + 1;
            } catch (NumberFormatException ignored) {
                seq = 1;
            }
        }
        return head + "-" + String.format("%03d", seq);
    }

    @Override
    public Map<String, Object> pageQuery(AnnualBudgetQuery query) {
        AnnualBudgetQuery q = query == null ? new AnnualBudgetQuery() : query;
        Pageable pageable = PageRequest.of(q.zeroBasedPage(), q.safePageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AnnualBudget> pageResult = annualBudgetRepository.findAll(buildSpec(q), pageable);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", pageResult.getContent().stream().map(this::toDto).collect(Collectors.toList()));
        result.put("total", pageResult.getTotalElements());
        result.put("current", q.zeroBasedPage() + 1);
        result.put("size", q.safePageSize());
        return result;
    }

    /** 多条件规格（Specification 避免 null 参数在 JPQL 中的类型推断问题） */
    private Specification<AnnualBudget> buildSpec(AnnualBudgetQuery q) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.notEqual(root.get("deleted"), true));

            String keyword = trimToNull(q.getKeyword());
            if (keyword != null) {
                predicates.add(cb.or(
                        cb.like(root.get("budgetNo"), "%" + keyword + "%"),
                        cb.like(root.get("departmentName"), "%" + keyword + "%"),
                        cb.like(root.get("description"), "%" + keyword + "%")));
            }
            String budgetNo = trimToNull(q.getBudgetNo());
            if (budgetNo != null) {
                predicates.add(cb.like(root.get("budgetNo"), "%" + budgetNo + "%"));
            }
            if (q.getFiscalYear() != null) {
                predicates.add(cb.equal(root.get("fiscalYear"), q.getFiscalYear()));
            }
            String deptName = trimToNull(q.getDepartmentName());
            if (deptName != null) {
                predicates.add(cb.like(root.get("departmentName"), "%" + deptName + "%"));
            }
            String deptId = trimToNull(q.getDepartmentId());
            if (deptId != null) {
                predicates.add(cb.equal(root.get("departmentId"), deptId));
            }
            String creatorName = trimToNull(q.getCreatorName());
            if (creatorName != null) {
                predicates.add(cb.like(root.get("creatorName"), "%" + creatorName + "%"));
            }
            String handlerName = trimToNull(q.getHandlerName());
            if (handlerName != null) {
                predicates.add(cb.like(root.get("handlerName"), "%" + handlerName + "%"));
            }
            String auditorName = trimToNull(q.getAuditorName());
            if (auditorName != null) {
                predicates.add(cb.like(root.get("auditorName"), "%" + auditorName + "%"));
            }
            String status = trimToNull(q.getStatus());
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            String statusIn = trimToNull(q.getStatusIn());
            if (statusIn != null) {
                List<String> statuses = Arrays.stream(statusIn.split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
                if (!statuses.isEmpty()) {
                    predicates.add(root.get("status").in(statuses));
                }
            }
            String remark = trimToNull(q.getRemark());
            if (remark != null) {
                predicates.add(cb.like(root.get("remark"), "%" + remark + "%"));
            }
            String description = trimToNull(q.getDescription());
            if (description != null) {
                predicates.add(cb.like(root.get("description"), "%" + description + "%"));
            }
            if (q.getDateStart() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("budgetDate"), q.getDateStart()));
            }
            if (q.getDateEnd() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("budgetDate"), q.getDateEnd()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    @Transactional
    public int batchDelete(List<Long> ids) {
        int ok = 0;
        for (Long id : safeIds(ids)) {
            AnnualBudget entity = annualBudgetRepository.findById(id).orElse(null);
            if (entity == null || entity.isDeleted()) {
                continue;
            }
            if (!"draft".equals(entity.getStatus()) && !"rejected".equals(entity.getStatus())) {
                continue;
            }
            entity.markAsDeleted();
            annualBudgetRepository.save(entity);
            ok++;
        }
        return ok;
    }

    @Override
    @Transactional
    public int batchSubmit(List<Long> ids) {
        int ok = 0;
        for (Long id : safeIds(ids)) {
            AnnualBudget entity = annualBudgetRepository.findById(id).orElse(null);
            if (entity == null || entity.isDeleted()) {
                continue;
            }
            if (!"draft".equals(entity.getStatus()) && !"rejected".equals(entity.getStatus())) {
                continue;
            }
            entity.setStatus("submitted");
            annualBudgetRepository.save(entity);
            ok++;
        }
        return ok;
    }

    @Override
    @Transactional
    public int batchApprove(List<Long> ids, Long auditorId, String auditorName, String auditRemark) {
        int ok = 0;
        for (Long id : safeIds(ids)) {
            AnnualBudget entity = annualBudgetRepository.findById(id).orElse(null);
            if (entity == null || entity.isDeleted() || !"submitted".equals(entity.getStatus())) {
                continue;
            }
            entity.setStatus("approved");
            entity.setTotalApprovedAmount(entity.getTotalAmount());
            entity.setAuditorId(auditorId);
            entity.setAuditorName(auditorName);
            entity.setAuditTime(LocalDateTime.now());
            entity.setAuditRemark(auditRemark);
            annualBudgetRepository.save(entity);
            ok++;
        }
        return ok;
    }

    @Override
    @Transactional
    public int batchReject(List<Long> ids, Long auditorId, String auditorName, String auditRemark) {
        int ok = 0;
        for (Long id : safeIds(ids)) {
            AnnualBudget entity = annualBudgetRepository.findById(id).orElse(null);
            if (entity == null || entity.isDeleted() || !"submitted".equals(entity.getStatus())) {
                continue;
            }
            entity.setStatus("rejected");
            entity.setAuditorId(auditorId);
            entity.setAuditorName(auditorName);
            entity.setAuditTime(LocalDateTime.now());
            entity.setAuditRemark(auditRemark);
            annualBudgetRepository.save(entity);
            ok++;
        }
        return ok;
    }

    @Override
    @Transactional
    public void increasePrintCount(Long id) {
        AnnualBudget entity = annualBudgetRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("年度预算不存在: " + id));
        entity.setPrintCount((entity.getPrintCount() == null ? 0 : entity.getPrintCount()) + 1);
        annualBudgetRepository.save(entity);
    }

    private AnnualBudgetDTO toDto(AnnualBudget entity) {
        AnnualBudgetDTO dto = new AnnualBudgetDTO();
        BeanUtil.copyProperties(entity, dto);
        return dto;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<Long> safeIds(List<Long> ids) {
        return ids == null ? Collections.emptyList()
                : ids.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }
}
