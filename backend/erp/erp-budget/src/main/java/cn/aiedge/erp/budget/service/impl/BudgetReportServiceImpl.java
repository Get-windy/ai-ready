package cn.aiedge.erp.budget.service.impl;

import cn.aiedge.erp.budget.dto.BudgetStatisticsDTO;
import cn.aiedge.erp.budget.model.AnnualBudget;
import cn.aiedge.erp.budget.model.BudgetExecutionLog;
import cn.aiedge.erp.budget.model.BudgetItem;
import cn.aiedge.erp.budget.repository.AnnualBudgetRepository;
import cn.aiedge.erp.budget.repository.BudgetExecutionLogRepository;
import cn.aiedge.erp.budget.repository.BudgetItemRepository;
import cn.aiedge.erp.budget.service.BudgetReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetReportServiceImpl implements BudgetReportService {

    private final AnnualBudgetRepository annualBudgetRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final BudgetExecutionLogRepository budgetExecutionLogRepository;

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    /** 已转入执行的预算状态：只有这些预算计入执行口径 */
    private static final java.util.Set<String> EXECUTED_STATUS =
            java.util.Set.of("approved", "executing", "closed");

    @Override
    public BudgetStatisticsDTO getExecutionSummary(Integer fiscalYear) {
        List<AnnualBudget> budgets = annualBudgetRepository.findByFiscalYearAndDeletedFalse(fiscalYear);

        BudgetStatisticsDTO dto = new BudgetStatisticsDTO();
        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalUsed = BigDecimal.ZERO;
        BigDecimal totalFrozen = BigDecimal.ZERO;
        BigDecimal overAmount = BigDecimal.ZERO;
        int totalCount = 0;
        int executingCount = 0;
        int closedCount = 0;
        int draftCount = 0;
        int approvedCount = 0;
        int itemCount = 0;
        int overCount = 0;
        int warnCount = 0;

        for (AnnualBudget budget : budgets) {
            String status = budget.getStatus() != null ? budget.getStatus() : "";
            switch (status) {
                case "executing": executingCount++; break;
                case "closed": closedCount++; break;
                case "draft": draftCount++; break;
                default: break;
            }
            // P1 预算来源：只有已审批并转入执行的预算才计入执行口径
            if (!EXECUTED_STATUS.contains(status)) {
                continue;
            }
            approvedCount++;

            // 金额按预算科目明细实时聚合（剩余 = 预算 − 已执行 − 冻结）
            for (BudgetItem item : budgetItemRepository.findByBudgetIdOrderBySortOrderAsc(budget.getId())) {
                itemCount++;
                BigDecimal amount = nvl(item.getBudgetAmount());
                BigDecimal used = nvl(item.getUsedAmount());
                BigDecimal frozen = nvl(item.getFrozenAmount());
                totalBudget = totalBudget.add(amount);
                totalUsed = totalUsed.add(used);
                totalFrozen = totalFrozen.add(frozen);
                BigDecimal rate = rate(used, amount);
                if (rate.compareTo(BigDecimal.valueOf(100)) > 0) {
                    overCount++;
                    overAmount = overAmount.add(used.subtract(amount).max(BigDecimal.ZERO));
                } else if (rate.compareTo(BigDecimal.valueOf(90)) >= 0) {
                    warnCount++;
                }
            }
        }
        totalCount = budgets.size();

        dto.setTotalBudgetAmount(totalBudget);
        dto.setTotalUsedAmount(totalUsed);
        dto.setTotalFrozenAmount(totalFrozen);
        dto.setTotalRemainingAmount(totalBudget.subtract(totalUsed).subtract(totalFrozen));
        dto.setExecutionRate(rate(totalUsed, totalBudget));
        dto.setTotalBudgetCount(totalCount);
        dto.setExecutingCount(executingCount);
        dto.setClosedCount(closedCount);
        dto.setDraftCount(draftCount);
        dto.setApprovedCount(approvedCount);
        dto.setTotalItemCount(itemCount);
        dto.setOverBudgetCount(overCount);
        dto.setWarningCount(warnCount);
        dto.setOverBudgetAmount(overAmount);

        return dto;
    }

    @Override
    public List<Map<String, Object>> getDepartmentSummary(Integer fiscalYear) {
        List<AnnualBudget> budgets = annualBudgetRepository.findByFiscalYearAndDeletedFalse(fiscalYear);

        Map<String, Map<String, Object>> deptMap = new LinkedHashMap<>();
        for (AnnualBudget budget : budgets) {
            if (!EXECUTED_STATUS.contains(budget.getStatus())) {
                continue;
            }
            String deptId = budget.getDepartmentId();
            String deptName = budget.getDepartmentName();
            if (deptId == null) continue;

            deptMap.putIfAbsent(deptId, new HashMap<>());
            Map<String, Object> entry = deptMap.get(deptId);
            entry.put("departmentId", deptId);
            entry.put("departmentName", deptName != null ? deptName : deptId);

            for (BudgetItem item : budgetItemRepository.findByBudgetIdOrderBySortOrderAsc(budget.getId())) {
                entry.merge("totalBudget", nvl(item.getBudgetAmount()), (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
                entry.merge("totalUsed", nvl(item.getUsedAmount()), (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
                entry.merge("totalFrozen", nvl(item.getFrozenAmount()), (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
            }
        }

        for (Map<String, Object> entry : deptMap.values()) {
            BigDecimal totalBudget = nvl((BigDecimal) entry.get("totalBudget"));
            BigDecimal totalUsed = nvl((BigDecimal) entry.get("totalUsed"));
            BigDecimal totalFrozen = nvl((BigDecimal) entry.get("totalFrozen"));
            entry.put("totalBudget", totalBudget);
            entry.put("totalUsed", totalUsed);
            entry.put("totalFrozen", totalFrozen);
            entry.put("executionRate", rate(totalUsed, totalBudget));
            // P1 剩余公式：剩余 = 预算 − 已执行 − 冻结
            entry.put("totalRemaining", totalBudget.subtract(totalUsed).subtract(totalFrozen));
        }

        return new ArrayList<>(deptMap.values());
    }

    @Override
    public List<Map<String, Object>> getSubjectSummary(Integer fiscalYear, Long budgetId) {
        List<BudgetItem> items;
        if (budgetId != null) {
            items = budgetItemRepository.findByBudgetIdOrderBySortOrderAsc(budgetId);
        } else {
            List<AnnualBudget> budgets = annualBudgetRepository.findByFiscalYearAndDeletedFalse(fiscalYear);
            items = new ArrayList<>();
            for (AnnualBudget budget : budgets) {
                if (!EXECUTED_STATUS.contains(budget.getStatus())) {
                    continue;
                }
                items.addAll(budgetItemRepository.findByBudgetIdOrderBySortOrderAsc(budget.getId()));
            }
        }

        Map<String, Map<String, Object>> subjectMap = new LinkedHashMap<>();
        for (BudgetItem item : items) {
            String code = item.getSubjectCode();
            if (code == null) code = "unknown";
            subjectMap.putIfAbsent(code, new HashMap<>());
            Map<String, Object> entry = subjectMap.get(code);
            entry.put("subjectCode", code);
            entry.put("subjectName", item.getSubjectName());

            entry.merge("totalBudget", nvl(item.getBudgetAmount()), (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
            entry.merge("totalUsed", nvl(item.getUsedAmount()), (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
            entry.merge("totalFrozen", nvl(item.getFrozenAmount()), (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
        }

        for (Map<String, Object> entry : subjectMap.values()) {
            BigDecimal totalBudget = nvl((BigDecimal) entry.get("totalBudget"));
            BigDecimal totalUsed = nvl((BigDecimal) entry.get("totalUsed"));
            BigDecimal totalFrozen = nvl((BigDecimal) entry.get("totalFrozen"));
            entry.put("totalBudget", totalBudget);
            entry.put("totalUsed", totalUsed);
            entry.put("totalFrozen", totalFrozen);
            entry.put("executionRate", rate(totalUsed, totalBudget));
            entry.put("totalRemaining", totalBudget.subtract(totalUsed).subtract(totalFrozen));
        }

        return new ArrayList<>(subjectMap.values());
    }

    @Override
    public List<Map<String, Object>> getVarianceAnalysis(Integer fiscalYear) {
        List<AnnualBudget> budgets = annualBudgetRepository.findByFiscalYearAndDeletedFalse(fiscalYear);
        List<Map<String, Object>> result = new ArrayList<>();

        for (AnnualBudget budget : budgets) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("budgetId", budget.getId());
            entry.put("budgetNo", budget.getBudgetNo());
            entry.put("departmentName", budget.getDepartmentName());
            entry.put("totalAmount", budget.getTotalAmount());
            entry.put("totalUsedAmount", budget.getTotalUsedAmount());
            entry.put("totalRemainingAmount", budget.getTotalRemainingAmount());
            entry.put("executionRate", budget.getExecutionRate());

            BigDecimal variance = BigDecimal.ZERO;
            if (budget.getTotalAmount() != null && budget.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                variance = budget.getTotalUsedAmount().subtract(budget.getTotalAmount());
            }
            entry.put("variance", variance);

            if (budget.getTotalAmount() != null && budget.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal varianceRate = variance.multiply(BigDecimal.valueOf(100))
                        .divide(budget.getTotalAmount(), 2, java.math.RoundingMode.HALF_UP);
                entry.put("varianceRate", varianceRate);
            } else {
                entry.put("varianceRate", BigDecimal.ZERO);
            }

            result.add(entry);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getTrend(Integer fiscalYear) {
        List<Map<String, Object>> result = new ArrayList<>();

        List<BudgetExecutionLog> logs = budgetExecutionLogRepository.findByExecutionDateBetweenOrderByExecutionDateAsc(
                LocalDate.of(fiscalYear, 1, 1), LocalDate.of(fiscalYear, 12, 31));

        Map<Integer, BigDecimal> monthlyMap = new TreeMap<>();
        for (int i = 1; i <= 12; i++) {
            monthlyMap.put(i, ZERO);
        }

        for (BudgetExecutionLog log : logs) {
            if (log.getExecutionDate() != null && "consume".equals(log.getExecutionType())) {
                int month = log.getExecutionDate().getMonthValue();
                monthlyMap.merge(month, nvl(log.getAmount()), BigDecimal::add);
            }
        }

        for (Map.Entry<Integer, BigDecimal> entry : monthlyMap.entrySet()) {
            Map<String, Object> point = new HashMap<>();
            point.put("month", entry.getKey());
            point.put("amount", entry.getValue());
            result.add(point);
        }

        return result;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : ZERO;
    }

    private static BigDecimal rate(BigDecimal used, BigDecimal amount) {
        if (nvl(amount).compareTo(ZERO) > 0) {
            return nvl(used).multiply(HUNDRED).divide(amount, 2, java.math.RoundingMode.HALF_UP);
        }
        return ZERO;
    }
}
