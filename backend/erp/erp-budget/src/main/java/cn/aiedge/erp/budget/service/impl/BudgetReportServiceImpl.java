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

    @Override
    public BudgetStatisticsDTO getExecutionSummary(Integer fiscalYear) {
        List<AnnualBudget> budgets = annualBudgetRepository.findByFiscalYearAndDeletedFalse(fiscalYear);

        BudgetStatisticsDTO dto = new BudgetStatisticsDTO();
        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalUsed = BigDecimal.ZERO;
        BigDecimal totalRemaining = BigDecimal.ZERO;
        int totalCount = 0;
        int executingCount = 0;
        int closedCount = 0;
        int draftCount = 0;

        for (AnnualBudget budget : budgets) {
            totalBudget = totalBudget.add(budget.getTotalAmount() != null ? budget.getTotalAmount() : BigDecimal.ZERO);
            totalUsed = totalUsed.add(budget.getTotalUsedAmount() != null ? budget.getTotalUsedAmount() : BigDecimal.ZERO);
            totalRemaining = totalRemaining.add(budget.getTotalRemainingAmount() != null ? budget.getTotalRemainingAmount() : BigDecimal.ZERO);
            totalCount++;
            switch (budget.getStatus()) {
                case "executing": executingCount++; break;
                case "closed": closedCount++; break;
                case "draft": draftCount++; break;
            }
        }

        dto.setTotalBudgetAmount(totalBudget);
        dto.setTotalUsedAmount(totalUsed);
        dto.setTotalRemainingAmount(totalRemaining);
        dto.setTotalBudgetCount(totalCount);
        dto.setExecutingCount(executingCount);
        dto.setClosedCount(closedCount);
        dto.setDraftCount(draftCount);

        if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
            dto.setExecutionRate(totalUsed.multiply(BigDecimal.valueOf(100))
                    .divide(totalBudget, 2, java.math.RoundingMode.HALF_UP));
        } else {
            dto.setExecutionRate(BigDecimal.ZERO);
        }

        return dto;
    }

    @Override
    public List<Map<String, Object>> getDepartmentSummary(Integer fiscalYear) {
        List<AnnualBudget> budgets = annualBudgetRepository.findByFiscalYearAndDeletedFalse(fiscalYear);

        Map<String, Map<String, Object>> deptMap = new LinkedHashMap<>();
        for (AnnualBudget budget : budgets) {
            String deptId = budget.getDepartmentId();
            String deptName = budget.getDepartmentName();
            if (deptId == null) continue;

            deptMap.putIfAbsent(deptId, new HashMap<>());
            Map<String, Object> entry = deptMap.get(deptId);
            entry.put("departmentId", deptId);
            entry.put("departmentName", deptName != null ? deptName : deptId);

            BigDecimal amount = budget.getTotalAmount() != null ? budget.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal used = budget.getTotalUsedAmount() != null ? budget.getTotalUsedAmount() : BigDecimal.ZERO;
            entry.merge("totalBudget", amount, (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
            entry.merge("totalUsed", used, (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
        }

        for (Map<String, Object> entry : deptMap.values()) {
            BigDecimal totalBudget = (BigDecimal) entry.get("totalBudget");
            BigDecimal totalUsed = (BigDecimal) entry.get("totalUsed");
            if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
                entry.put("executionRate", totalUsed.multiply(BigDecimal.valueOf(100))
                        .divide(totalBudget, 2, java.math.RoundingMode.HALF_UP));
            } else {
                entry.put("executionRate", BigDecimal.ZERO);
            }
            entry.put("totalRemaining", totalBudget.subtract(totalUsed));
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

            BigDecimal amount = item.getBudgetAmount() != null ? item.getBudgetAmount() : BigDecimal.ZERO;
            BigDecimal used = item.getUsedAmount() != null ? item.getUsedAmount() : BigDecimal.ZERO;
            entry.merge("totalBudget", amount, (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
            entry.merge("totalUsed", used, (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
        }

        for (Map<String, Object> entry : subjectMap.values()) {
            BigDecimal totalBudget = (BigDecimal) entry.get("totalBudget");
            BigDecimal totalUsed = (BigDecimal) entry.get("totalUsed");
            if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
                entry.put("executionRate", totalUsed.multiply(BigDecimal.valueOf(100))
                        .divide(totalBudget, 2, java.math.RoundingMode.HALF_UP));
            } else {
                entry.put("executionRate", BigDecimal.ZERO);
            }
            entry.put("totalRemaining", totalBudget.subtract(totalUsed));
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

        List<BudgetExecutionLog> logs = budgetExecutionLogRepository.findByBudgetIdAndExecutionDateBetweenOrderByExecutionDateDesc(
                null, LocalDate.of(fiscalYear, 1, 1), LocalDate.of(fiscalYear, 12, 31));

        Map<Integer, BigDecimal> monthlyMap = new TreeMap<>();
        for (int i = 1; i <= 12; i++) {
            monthlyMap.put(i, BigDecimal.ZERO);
        }

        for (BudgetExecutionLog log : logs) {
            if (log.getExecutionDate() != null && "consume".equals(log.getExecutionType())) {
                int month = log.getExecutionDate().getMonthValue();
                BigDecimal amount = log.getAmount() != null ? log.getAmount() : BigDecimal.ZERO;
                monthlyMap.merge(month, amount, BigDecimal::add);
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
}
