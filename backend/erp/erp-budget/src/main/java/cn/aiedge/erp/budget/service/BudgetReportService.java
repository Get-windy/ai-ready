package cn.aiedge.erp.budget.service;

import cn.aiedge.erp.budget.dto.BudgetStatisticsDTO;

import java.util.List;
import java.util.Map;

public interface BudgetReportService {

    BudgetStatisticsDTO getExecutionSummary(Integer fiscalYear);

    List<Map<String, Object>> getDepartmentSummary(Integer fiscalYear);

    List<Map<String, Object>> getSubjectSummary(Integer fiscalYear, Long budgetId);

    List<Map<String, Object>> getVarianceAnalysis(Integer fiscalYear);

    List<Map<String, Object>> getTrend(Integer fiscalYear);
}
