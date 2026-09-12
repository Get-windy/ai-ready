package cn.aiedge.erp.budget.service;

import cn.aiedge.erp.budget.dto.BudgetExecutionLogVO;
import cn.aiedge.erp.budget.dto.BudgetExecutionQuery;

import java.util.List;
import java.util.Map;

/**
 * 预算执行跟踪（只读）
 *
 * <p><b>P0 红线</b>：本服务只读取自《预算计划》的预算科目与本服务记录的已记账支出，
 * 严禁任何直接修改已执行 / 剩余额的入口；所有金额均按明细行实时聚合，
 * 保证「剩余 = 预算 − 已执行 − 冻结」恒成立。</p>
 */
public interface BudgetExecutionService {

    /** 按预算单维度分页 */
    Map<String, Object> pageRows(BudgetExecutionQuery query);

    /** 按预算科目明细维度分页 */
    Map<String, Object> pageItems(BudgetExecutionQuery query);

    /** 预算执行流水（冻结 / 释放 / 消耗） */
    List<BudgetExecutionLogVO> logs(Long budgetId, Long budgetItemId);

    /** 超支预警清单（执行进度 > 100%）与接近额度清单（>= 90%） */
    Map<String, Object> warnings(Integer fiscalYear);
}
