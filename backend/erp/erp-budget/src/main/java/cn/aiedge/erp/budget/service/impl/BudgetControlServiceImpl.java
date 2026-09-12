package cn.aiedge.erp.budget.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.budget.model.BudgetExecutionLog;
import cn.aiedge.erp.budget.model.BudgetItem;
import cn.aiedge.erp.budget.repository.BudgetExecutionLogRepository;
import cn.aiedge.erp.budget.repository.BudgetItemRepository;
import cn.aiedge.erp.budget.service.AnnualBudgetService;
import cn.aiedge.erp.budget.service.BudgetControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 预算控制服务：冻结 / 释放 / 消耗 预算科目额度。
 *
 * <p><b>P0 口径（红线）</b>：预算科目三列为唯一事实来源，恒等式必须成立 ——</p>
 * <pre>
 *   remaining_amount = budget_amount − used_amount − frozen_amount
 * </pre>
 * <p>冻结与消耗只允许经本服务变更，禁止任何页面/接口直接改写已执行、冻结或剩余额；
 * 变更后统一回写 annual_budget 汇总（已执行 / 冻结 / 剩余 / 执行率），
 * 保证《预算执行》概览与明细口径一致。</p>
 */
@Service
@RequiredArgsConstructor
public class BudgetControlServiceImpl implements BudgetControlService {

    private final BudgetItemRepository budgetItemRepository;
    private final BudgetExecutionLogRepository budgetExecutionLogRepository;
    private final AnnualBudgetService annualBudgetService;

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Override
    public Map<String, Object> checkAvailability(Long budgetId, Long budgetItemId, BigDecimal amount) {
        Map<String, Object> result = new HashMap<>();
        result.put("available", false);
        result.put("budgetId", budgetId);
        result.put("amount", amount);

        BudgetItem item = budgetItemRepository.findById(budgetItemId).orElse(null);
        if (item == null) {
            result.put("message", "预算科目不存在");
            return result;
        }

        // remaining 已扣减冻结，可直接作为可用额度
        BigDecimal remaining = nvl(item.getRemainingAmount());
        BigDecimal frozen = nvl(item.getFrozenAmount());

        result.put("remainingAmount", remaining);
        result.put("frozenAmount", frozen);
        result.put("availableAmount", remaining);

        if (remaining.compareTo(nvl(amount)) >= 0) {
            result.put("available", true);
            result.put("message", "预算充足");
        } else {
            result.put("available", false);
            result.put("message", "预算不足，可用余额: " + remaining + ", 需要: " + amount);
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> freezeAmount(Long budgetId, Long budgetItemId, BigDecimal amount,
                                            String sourceType, String sourceNo, Long sourceId) {
        BigDecimal freeze = nvl(amount);
        Map<String, Object> result = new HashMap<>();

        BudgetItem item = budgetItemRepository.findById(budgetItemId)
                .orElseThrow(() -> BusinessException.notFound("预算科目不存在: " + budgetItemId));

        BigDecimal remaining = nvl(item.getRemainingAmount());
        if (remaining.compareTo(freeze) < 0) {
            throw BusinessException.badRequest("预算不足，无法冻结");
        }

        item.setFrozenAmount(nvl(item.getFrozenAmount()).add(freeze));
        applyItemAmounts(item);
        item.setLastExecDate(LocalDate.now());
        budgetItemRepository.save(item);

        writeLog(budgetId, budgetItemId, freeze, "freeze", sourceType, sourceNo, sourceId,
                "冻结预算: " + freeze);
        annualBudgetService.recalculateSummary(budgetId);

        result.put("success", true);
        result.put("message", "预算冻结成功");
        result.put("frozenAmount", item.getFrozenAmount());
        result.put("remainingAmount", item.getRemainingAmount());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> releaseFrozenAmount(Long budgetId, Long budgetItemId, BigDecimal amount,
                                                   String sourceType, String sourceNo, Long sourceId) {
        BigDecimal release = nvl(amount);
        Map<String, Object> result = new HashMap<>();

        BudgetItem item = budgetItemRepository.findById(budgetItemId)
                .orElseThrow(() -> BusinessException.notFound("预算科目不存在: " + budgetItemId));

        BigDecimal frozen = nvl(item.getFrozenAmount());
        if (frozen.compareTo(release) < 0) {
            throw BusinessException.badRequest("冻结余额不足，无法释放");
        }

        item.setFrozenAmount(frozen.subtract(release));
        applyItemAmounts(item);
        item.setLastExecDate(LocalDate.now());
        budgetItemRepository.save(item);

        writeLog(budgetId, budgetItemId, release, "unfreeze", sourceType, sourceNo, sourceId,
                "释放冻结预算: " + release);
        annualBudgetService.recalculateSummary(budgetId);

        result.put("success", true);
        result.put("message", "预算冻结释放成功");
        result.put("frozenAmount", item.getFrozenAmount());
        result.put("remainingAmount", item.getRemainingAmount());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> consumeBudget(Long budgetId, Long budgetItemId, BigDecimal amount,
                                             String sourceType, String sourceNo, Long sourceId, String description) {
        return consumeBudget(budgetId, budgetItemId, amount, sourceType, sourceNo, sourceId, description, false);
    }

    @Override
    @Transactional
    public Map<String, Object> consumeBudget(Long budgetId, Long budgetItemId, BigDecimal amount,
                                             String sourceType, String sourceNo, Long sourceId, String description,
                                             boolean allowOverBudget) {
        BigDecimal consume = nvl(amount);
        Map<String, Object> result = new HashMap<>();

        BudgetItem item = budgetItemRepository.findById(budgetItemId)
                .orElseThrow(() -> BusinessException.notFound("预算科目不存在: " + budgetItemId));

        BigDecimal remaining = nvl(item.getRemainingAmount());
        if (!allowOverBudget && remaining.compareTo(consume) < 0) {
            throw BusinessException.badRequest("预算余额不足，无法消耗");
        }

        // 消耗：占用中的冻结额度同步释放，未冻结部分直接冲减剩余
        BigDecimal frozen = nvl(item.getFrozenAmount());
        item.setFrozenAmount(frozen.subtract(frozen.min(consume)));
        item.setUsedAmount(nvl(item.getUsedAmount()).add(consume));
        applyItemAmounts(item);
        item.setLastExecDate(LocalDate.now());
        budgetItemRepository.save(item);

        writeLog(budgetId, budgetItemId, consume, "consume", sourceType, sourceNo, sourceId,
                description != null ? description : "消耗预算: " + consume);
        annualBudgetService.recalculateSummary(budgetId);

        boolean overBudget = nvl(item.getExecutionRate()).compareTo(BigDecimal.valueOf(100)) > 0;
        result.put("success", true);
        result.put("message", overBudget ? "预算消耗成功（已超支）" : "预算消耗成功");
        result.put("usedAmount", item.getUsedAmount());
        result.put("frozenAmount", item.getFrozenAmount());
        result.put("remainingAmount", item.getRemainingAmount());
        result.put("executionRate", item.getExecutionRate());
        result.put("overBudget", overBudget);
        return result;
    }

    /**
     * 依据「剩余 = 预算 − 已执行 − 冻结」重算科目剩余额与执行率。
     * 所有变更已执行/冻结的操作都必须经过此处，保证恒等式不被破坏。
     */
    private void applyItemAmounts(BudgetItem item) {
        BigDecimal budget = nvl(item.getBudgetAmount());
        BigDecimal used = nvl(item.getUsedAmount());
        BigDecimal frozen = nvl(item.getFrozenAmount());
        item.setRemainingAmount(budget.subtract(used).subtract(frozen));
        if (budget.compareTo(ZERO) > 0) {
            item.setExecutionRate(used.multiply(BigDecimal.valueOf(100)).divide(budget, 2, RoundingMode.HALF_UP));
        } else {
            item.setExecutionRate(ZERO);
        }
    }

    private void writeLog(Long budgetId, Long budgetItemId, BigDecimal amount, String executionType,
                          String sourceType, String sourceNo, Long sourceId, String description) {
        BudgetExecutionLog log = new BudgetExecutionLog();
        log.setBudgetId(budgetId);
        log.setBudgetItemId(budgetItemId);
        log.setSourceType(sourceType);
        log.setSourceNo(sourceNo);
        log.setSourceId(sourceId);
        log.setAmount(amount);
        log.setExecutionType(executionType);
        log.setExecutionDate(LocalDate.now());
        log.setDescription(description);
        budgetExecutionLogRepository.save(log);
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : ZERO;
    }
}
