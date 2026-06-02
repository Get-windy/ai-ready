package cn.aiedge.erp.budget.service.impl;

import cn.aiedge.erp.budget.model.BudgetExecutionLog;
import cn.aiedge.erp.budget.model.BudgetItem;
import cn.aiedge.erp.budget.repository.BudgetExecutionLogRepository;
import cn.aiedge.erp.budget.repository.BudgetItemRepository;
import cn.aiedge.erp.budget.service.BudgetControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BudgetControlServiceImpl implements BudgetControlService {

    private final BudgetItemRepository budgetItemRepository;
    private final BudgetExecutionLogRepository budgetExecutionLogRepository;

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

        BigDecimal remaining = item.getRemainingAmount() != null ? item.getRemainingAmount() : BigDecimal.ZERO;
        BigDecimal frozen = item.getFrozenAmount() != null ? item.getFrozenAmount() : BigDecimal.ZERO;
        BigDecimal available = remaining.subtract(frozen);

        result.put("remainingAmount", remaining);
        result.put("frozenAmount", frozen);
        result.put("availableAmount", available);

        if (available.compareTo(amount) >= 0) {
            result.put("available", true);
            result.put("message", "预算充足");
        } else {
            result.put("available", false);
            result.put("message", "预算不足，可用余额: " + available + ", 需要: " + amount);
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> freezeAmount(Long budgetId, Long budgetItemId, BigDecimal amount,
                                            String sourceType, String sourceNo, Long sourceId) {
        Map<String, Object> result = new HashMap<>();

        BudgetItem item = budgetItemRepository.findById(budgetItemId)
                .orElseThrow(() -> new RuntimeException("预算科目不存在: " + budgetItemId));

        BigDecimal remaining = item.getRemainingAmount() != null ? item.getRemainingAmount() : BigDecimal.ZERO;
        BigDecimal frozen = item.getFrozenAmount() != null ? item.getFrozenAmount() : BigDecimal.ZERO;

        if (remaining.subtract(frozen).compareTo(amount) < 0) {
            throw new RuntimeException("预算不足，无法冻结");
        }

        item.setFrozenAmount(frozen.add(amount));
        budgetItemRepository.save(item);

        BudgetExecutionLog log = new BudgetExecutionLog();
        log.setBudgetId(budgetId);
        log.setBudgetItemId(budgetItemId);
        log.setSourceType(sourceType);
        log.setSourceNo(sourceNo);
        log.setSourceId(sourceId);
        log.setAmount(amount);
        log.setExecutionType("freeze");
        log.setExecutionDate(LocalDate.now());
        log.setDescription("冻结预算: " + amount);
        budgetExecutionLogRepository.save(log);

        result.put("success", true);
        result.put("message", "预算冻结成功");
        result.put("frozenAmount", item.getFrozenAmount());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> releaseFrozenAmount(Long budgetId, Long budgetItemId, BigDecimal amount,
                                                   String sourceType, String sourceNo, Long sourceId) {
        Map<String, Object> result = new HashMap<>();

        BudgetItem item = budgetItemRepository.findById(budgetItemId)
                .orElseThrow(() -> new RuntimeException("预算科目不存在: " + budgetItemId));

        BigDecimal frozen = item.getFrozenAmount() != null ? item.getFrozenAmount() : BigDecimal.ZERO;
        if (frozen.compareTo(amount) < 0) {
            throw new RuntimeException("冻结余额不足，无法释放");
        }

        item.setFrozenAmount(frozen.subtract(amount));
        budgetItemRepository.save(item);

        BudgetExecutionLog log = new BudgetExecutionLog();
        log.setBudgetId(budgetId);
        log.setBudgetItemId(budgetItemId);
        log.setSourceType(sourceType);
        log.setSourceNo(sourceNo);
        log.setSourceId(sourceId);
        log.setAmount(amount);
        log.setExecutionType("unfreeze");
        log.setExecutionDate(LocalDate.now());
        log.setDescription("释放冻结预算: " + amount);
        budgetExecutionLogRepository.save(log);

        result.put("success", true);
        result.put("message", "预算冻结释放成功");
        result.put("frozenAmount", item.getFrozenAmount());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> consumeBudget(Long budgetId, Long budgetItemId, BigDecimal amount,
                                             String sourceType, String sourceNo, Long sourceId, String description) {
        Map<String, Object> result = new HashMap<>();

        BudgetItem item = budgetItemRepository.findById(budgetItemId)
                .orElseThrow(() -> new RuntimeException("预算科目不存在: " + budgetItemId));

        BigDecimal remaining = item.getRemainingAmount() != null ? item.getRemainingAmount() : BigDecimal.ZERO;
        BigDecimal frozen = item.getFrozenAmount() != null ? item.getFrozenAmount() : BigDecimal.ZERO;

        // Try to consume from remaining first
        if (remaining.compareTo(amount) < 0) {
            throw new RuntimeException("预算余额不足，无法消耗");
        }

        BigDecimal actualFrozenRelease = frozen.min(amount);
        BigDecimal actualConsume = amount;

        item.setFrozenAmount(frozen.subtract(actualFrozenRelease));
        item.setRemainingAmount(remaining.subtract(actualConsume));
        item.setUsedAmount((item.getUsedAmount() != null ? item.getUsedAmount() : BigDecimal.ZERO).add(actualConsume));

        if (item.getBudgetAmount() != null && item.getBudgetAmount().compareTo(BigDecimal.ZERO) > 0) {
            item.setExecutionRate(item.getUsedAmount().multiply(BigDecimal.valueOf(100))
                    .divide(item.getBudgetAmount(), 2, java.math.RoundingMode.HALF_UP));
        }

        budgetItemRepository.save(item);

        BudgetExecutionLog log = new BudgetExecutionLog();
        log.setBudgetId(budgetId);
        log.setBudgetItemId(budgetItemId);
        log.setSourceType(sourceType);
        log.setSourceNo(sourceNo);
        log.setSourceId(sourceId);
        log.setAmount(amount);
        log.setExecutionType("consume");
        log.setExecutionDate(LocalDate.now());
        log.setDescription(description != null ? description : "消耗预算: " + amount);
        budgetExecutionLogRepository.save(log);

        result.put("success", true);
        result.put("message", "预算消耗成功");
        result.put("usedAmount", item.getUsedAmount());
        result.put("remainingAmount", item.getRemainingAmount());
        return result;
    }
}
