package cn.aiedge.erp.budget.service.impl;

import cn.aiedge.erp.budget.dto.BudgetExecutionItemDTO;
import cn.aiedge.erp.budget.dto.BudgetExecutionLogVO;
import cn.aiedge.erp.budget.dto.BudgetExecutionQuery;
import cn.aiedge.erp.budget.dto.BudgetExecutionRowDTO;
import cn.aiedge.erp.budget.model.AnnualBudget;
import cn.aiedge.erp.budget.model.BudgetExecutionLog;
import cn.aiedge.erp.budget.model.BudgetItem;
import cn.aiedge.erp.budget.repository.AnnualBudgetRepository;
import cn.aiedge.erp.budget.repository.BudgetExecutionLogRepository;
import cn.aiedge.erp.budget.repository.BudgetItemRepository;
import cn.aiedge.erp.budget.service.BudgetExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 预算执行跟踪实现
 *
 * <p>金额一律按 budget_item 明细行实时聚合，不直接采用 annual_budget 的汇总列，
 * 从实现层面杜绝「直改已执行 / 剩余额」与汇总漂移。</p>
 */
@Service
@RequiredArgsConstructor
public class BudgetExecutionServiceImpl implements BudgetExecutionService {

    private final AnnualBudgetRepository annualBudgetRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final BudgetExecutionLogRepository budgetExecutionLogRepository;

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal WARN_THRESHOLD = BigDecimal.valueOf(90);
    /** 默认口径：已转入执行的预算（已审批 / 执行中 / 已关闭） */
    private static final List<String> DEFAULT_STATUS = List.of("approved", "executing", "closed");

    private static final Map<String, String> STATUS_NAME = Map.of(
            "draft", "草稿",
            "submitted", "待审批",
            "approved", "已审批",
            "rejected", "已驳回",
            "executing", "执行中",
            "closed", "已关闭");

    private static final Map<String, String> EXEC_TYPE_NAME = Map.of(
            "freeze", "冻结",
            "unfreeze", "释放",
            "consume", "消耗");

    private static final Map<String, String> SOURCE_TYPE_NAME = Map.of(
            "expense", "费用单",
            "manual", "手工");

    @Override
    public Map<String, Object> pageRows(BudgetExecutionQuery query) {
        List<BudgetExecutionRowDTO> rows = new ArrayList<>();
        for (AnnualBudget budget : loadBudgets(query)) {
            rows.add(toRow(budget, budgetItemRepository.findByBudgetIdOrderBySortOrderAsc(budget.getId())));
        }
        return paged(rows, query);
    }

    @Override
    public Map<String, Object> pageItems(BudgetExecutionQuery query) {
        List<AnnualBudget> budgets = loadBudgets(query);
        Map<Long, AnnualBudget> budgetMap = budgets.stream()
                .collect(Collectors.toMap(AnnualBudget::getId, b -> b, (a, b) -> a));
        if (budgetMap.isEmpty()) {
            return paged(new ArrayList<>(), query);
        }

        Map<Long, Integer> execCounts = countsByItem(budgetMap.keySet());
        List<BudgetExecutionItemDTO> items = new ArrayList<>();
        for (BudgetItem item : budgetItemRepository.findByBudgetIdInAndDeletedFalse(budgetMap.keySet())) {
            AnnualBudget budget = budgetMap.get(item.getBudgetId());
            if (budget == null || !matchItem(item, budget, query)) {
                continue;
            }
            items.add(toItem(item, budget, execCounts.getOrDefault(item.getId(), 0)));
        }
        return paged(items, query);
    }

    @Override
    public List<BudgetExecutionLogVO> logs(Long budgetId, Long budgetItemId) {
        List<BudgetExecutionLog> logs;
        if (budgetItemId != null) {
            logs = budgetExecutionLogRepository.findByBudgetItemIdOrderByExecutionDateDesc(budgetItemId);
        } else if (budgetId != null) {
            logs = budgetExecutionLogRepository.findByBudgetIdOrderByExecutionDateDesc(budgetId);
        } else {
            return new ArrayList<>();
        }
        return logs.stream().map(this::toLogVO).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> warnings(Integer fiscalYear) {
        BudgetExecutionQuery query = new BudgetExecutionQuery();
        query.setFiscalYear(fiscalYear);
        List<AnnualBudget> budgets = loadBudgets(query);
        Map<Long, AnnualBudget> budgetMap = budgets.stream()
                .collect(Collectors.toMap(AnnualBudget::getId, b -> b, (a, b) -> a));

        List<BudgetExecutionItemDTO> all = new ArrayList<>();
        if (!budgetMap.isEmpty()) {
            Map<Long, Integer> execCounts = countsByItem(budgetMap.keySet());
            for (BudgetItem item : budgetItemRepository.findByBudgetIdInAndDeletedFalse(budgetMap.keySet())) {
                AnnualBudget budget = budgetMap.get(item.getBudgetId());
                if (budget != null) {
                    all.add(toItem(item, budget, execCounts.getOrDefault(item.getId(), 0)));
                }
            }
        }

        List<BudgetExecutionItemDTO> over = all.stream()
                .filter(i -> "over".equals(i.getWarnLevel()))
                .sorted(Comparator.comparing(BudgetExecutionItemDTO::getOverAmount, Comparator.reverseOrder()))
                .collect(Collectors.toList());
        List<BudgetExecutionItemDTO> near = all.stream()
                .filter(i -> "warn".equals(i.getWarnLevel()))
                .sorted(Comparator.comparing(BudgetExecutionItemDTO::getExecutionRate, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fiscalYear", fiscalYear);
        result.put("overBudgetCount", over.size());
        result.put("warningCount", near.size());
        result.put("overBudgetAmount", over.stream().map(BudgetExecutionItemDTO::getOverAmount)
                .reduce(ZERO, BigDecimal::add));
        result.put("overBudgetItems", over);
        result.put("warningItems", near);
        return result;
    }

    // ═══ 内部 ═══

    private List<AnnualBudget> loadBudgets(BudgetExecutionQuery query) {
        Integer fiscalYear = query.getFiscalYear() != null ? query.getFiscalYear() : LocalDate.now().getYear();
        Set<String> statuses = query.getStatus() != null && !query.getStatus().isBlank()
                ? Set.of(query.getStatus().trim().split(","))
                : new HashSet<>(DEFAULT_STATUS);

        return annualBudgetRepository.findByFiscalYearAndDeletedFalse(fiscalYear).stream()
                .filter(b -> statuses.contains(b.getStatus()))
                .filter(b -> query.getDepartmentId() == null || query.getDepartmentId().isBlank()
                        || query.getDepartmentId().equals(b.getDepartmentId()))
                .filter(b -> {
                    if (query.getKeyword() == null || query.getKeyword().isBlank()) {
                        return true;
                    }
                    String kw = query.getKeyword().trim();
                    return contains(b.getBudgetNo(), kw) || contains(b.getDepartmentName(), kw)
                            || contains(b.getDescription(), kw);
                })
                .sorted(Comparator.comparing(AnnualBudget::getId))
                .collect(Collectors.toList());
    }

    private boolean matchItem(BudgetItem item, AnnualBudget budget, BudgetExecutionQuery query) {
        if (query.getSubjectCode() != null && !query.getSubjectCode().isBlank()
                && !query.getSubjectCode().equals(item.getSubjectCode())) {
            return false;
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String kw = query.getKeyword().trim();
            return contains(item.getSubjectCode(), kw) || contains(item.getSubjectName(), kw)
                    || contains(budget.getBudgetNo(), kw) || contains(budget.getDepartmentName(), kw);
        }
        return true;
    }

    /** 一次性统计各预算科目的执行流水笔数，避免逐行查询 */
    private Map<Long, Integer> countsByItem(Collection<Long> budgetIds) {
        Map<Long, Integer> counts = new HashMap<>();
        for (BudgetExecutionLog log : budgetExecutionLogRepository.findByBudgetIdInOrderByExecutionDateDesc(budgetIds)) {
            if (log.getBudgetItemId() != null) {
                counts.merge(log.getBudgetItemId(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private BudgetExecutionRowDTO toRow(AnnualBudget budget, List<BudgetItem> items) {
        BigDecimal amount = ZERO;
        BigDecimal used = ZERO;
        BigDecimal frozen = ZERO;
        int overItemCount = 0;
        LocalDate lastExec = null;
        for (BudgetItem item : items) {
            amount = amount.add(nvl(item.getBudgetAmount()));
            used = used.add(nvl(item.getUsedAmount()));
            frozen = frozen.add(nvl(item.getFrozenAmount()));
            if (nvl(item.getExecutionRate()).compareTo(HUNDRED) > 0) {
                overItemCount++;
            }
            if (item.getLastExecDate() != null && (lastExec == null || item.getLastExecDate().isAfter(lastExec))) {
                lastExec = item.getLastExecDate();
            }
        }
        BigDecimal remaining = amount.subtract(used).subtract(frozen);

        BudgetExecutionRowDTO dto = new BudgetExecutionRowDTO();
        dto.setBudgetId(budget.getId());
        dto.setBudgetNo(budget.getBudgetNo());
        dto.setFiscalYear(budget.getFiscalYear());
        dto.setDepartmentId(budget.getDepartmentId());
        dto.setDepartmentName(budget.getDepartmentName());
        dto.setBudgetName(budget.getDescription());
        dto.setStatus(budget.getStatus());
        dto.setStatusName(STATUS_NAME.getOrDefault(budget.getStatus(), budget.getStatus()));
        dto.setTotalAmount(amount);
        dto.setUsedAmount(used);
        dto.setFrozenAmount(frozen);
        dto.setRemainingAmount(remaining);
        dto.setExecutionRate(rate(used, amount));
        dto.setItemCount(items.size());
        dto.setOverItemCount(overItemCount);
        dto.setLastExecDate(lastExec);
        fillWarn(dto::setOverBudget, dto::setWarnLevel, dto::setWarnMessage, used, amount, remaining);
        return dto;
    }

    private BudgetExecutionItemDTO toItem(BudgetItem item, AnnualBudget budget, int execCount) {
        BigDecimal amount = nvl(item.getBudgetAmount());
        BigDecimal used = nvl(item.getUsedAmount());
        BigDecimal frozen = nvl(item.getFrozenAmount());
        BigDecimal remaining = amount.subtract(used).subtract(frozen);

        BudgetExecutionItemDTO dto = new BudgetExecutionItemDTO();
        dto.setId(item.getId());
        dto.setBudgetId(item.getBudgetId());
        dto.setBudgetNo(budget.getBudgetNo());
        dto.setFiscalYear(budget.getFiscalYear());
        dto.setDepartmentId(budget.getDepartmentId());
        dto.setDepartmentName(budget.getDepartmentName());
        dto.setStatus(budget.getStatus());
        dto.setStatusName(STATUS_NAME.getOrDefault(budget.getStatus(), budget.getStatus()));
        dto.setLineNo(item.getSortOrder());
        dto.setSubjectCode(item.getSubjectCode());
        dto.setSubjectName(item.getSubjectName());
        dto.setBudgetAmount(amount);
        dto.setUsedAmount(used);
        dto.setFrozenAmount(frozen);
        dto.setRemainingAmount(remaining);
        dto.setExecutionRate(rate(used, amount));
        dto.setOverAmount(used.subtract(amount).max(ZERO));
        dto.setLastExecDate(item.getLastExecDate());
        dto.setExecCount(execCount);
        dto.setRemark(item.getRemark());
        fillWarn(dto::setOverBudget, dto::setWarnLevel, dto::setWarnMessage, used, amount, remaining);
        return dto;
    }

    /** 超支预警：执行进度 > 100% 超支；>= 90% 接近额度预警 */
    private void fillWarn(java.util.function.Consumer<Boolean> over,
                          java.util.function.Consumer<String> level,
                          java.util.function.Consumer<String> message,
                          BigDecimal used, BigDecimal amount, BigDecimal remaining) {
        BigDecimal rateValue = rate(used, amount);
        if (rateValue.compareTo(HUNDRED) > 0) {
            over.accept(true);
            level.accept("over");
            message.accept("已超支：执行进度 " + rateValue + "%，超支 " + used.subtract(amount).max(ZERO) + " 元");
        } else if (rateValue.compareTo(WARN_THRESHOLD) >= 0) {
            over.accept(false);
            level.accept("warn");
            message.accept("接近预算额度：执行进度 " + rateValue + "%，剩余 " + remaining + " 元");
        } else {
            over.accept(false);
            level.accept("normal");
        }
    }

    private BudgetExecutionLogVO toLogVO(BudgetExecutionLog log) {
        BudgetExecutionLogVO vo = new BudgetExecutionLogVO();
        vo.setId(log.getId());
        vo.setBudgetId(log.getBudgetId());
        vo.setBudgetItemId(log.getBudgetItemId());
        vo.setExecutionType(log.getExecutionType());
        vo.setExecutionTypeName(EXEC_TYPE_NAME.getOrDefault(log.getExecutionType(), log.getExecutionType()));
        vo.setSourceType(log.getSourceType());
        vo.setSourceTypeName(log.getSourceType() != null
                ? SOURCE_TYPE_NAME.getOrDefault(log.getSourceType(), log.getSourceType()) : null);
        vo.setSourceNo(log.getSourceNo());
        vo.setSourceId(log.getSourceId());
        vo.setAmount(log.getAmount());
        vo.setExecutionDate(log.getExecutionDate());
        vo.setDescription(log.getDescription());
        vo.setCreatedBy(log.getCreatedBy());
        vo.setCreatedAt(log.getCreatedAt());
        return vo;
    }

    /** 过滤 + 排序（超支优先、执行进度倒序）+ 内存分页 */
    private Map<String, Object> paged(List<?> rows, BudgetExecutionQuery query) {
        List<?> filtered = rows.stream()
                .filter(row -> {
                    BudgetExecutionRowDTO view = asView(row);
                    if (Boolean.TRUE.equals(query.getOverBudgetOnly()) && !Boolean.TRUE.equals(view.getOverBudget())) {
                        return false;
                    }
                    return !Boolean.TRUE.equals(query.getWarningOnly()) || !"normal".equals(view.getWarnLevel());
                })
                .sorted(Comparator
                        .comparing((Object row) -> Boolean.TRUE.equals(asView(row).getOverBudget()) ? 0 : 1)
                        .thenComparing(row -> asView(row).getExecutionRate() != null
                                ? asView(row).getExecutionRate() : ZERO, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        int total = filtered.size();
        int safeSize = query.getSize() > 0 ? query.getSize() : 20;
        int safePage = Math.max(query.getPage(), 0);
        int from = Math.min(safePage * safeSize, total);
        int to = Math.min(from + safeSize, total);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", new ArrayList<>(filtered.subList(from, to)));
        result.put("total", (long) total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    private BudgetExecutionRowDTO asView(Object row) {
        if (row instanceof BudgetExecutionRowDTO dto) {
            return dto;
        }
        BudgetExecutionItemDTO item = (BudgetExecutionItemDTO) row;
        BudgetExecutionRowDTO view = new BudgetExecutionRowDTO();
        view.setOverBudget(item.getOverBudget());
        view.setWarnLevel(item.getWarnLevel());
        view.setExecutionRate(item.getExecutionRate());
        return view;
    }

    private static BigDecimal rate(BigDecimal used, BigDecimal amount) {
        if (amount.compareTo(ZERO) > 0) {
            return used.multiply(HUNDRED).divide(amount, 2, RoundingMode.HALF_UP);
        }
        return ZERO;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : ZERO;
    }

    private static boolean contains(String text, String keyword) {
        return text != null && text.toLowerCase().contains(keyword.toLowerCase());
    }
}
