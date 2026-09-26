package cn.aiedge.erp.budget.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.budget.dto.BudgetItemDTO;
import cn.aiedge.erp.budget.model.BudgetItem;
import cn.aiedge.erp.budget.repository.BudgetItemRepository;
import cn.aiedge.erp.budget.service.BudgetItemService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetItemServiceImpl implements BudgetItemService {

    private final BudgetItemRepository budgetItemRepository;

    @Override
    public List<BudgetItemDTO> listByBudgetId(Long budgetId) {
        List<BudgetItem> items = budgetItemRepository.findByBudgetIdOrderBySortOrderAsc(budgetId);
        return items.stream().map(item -> {
            BudgetItemDTO dto = new BudgetItemDTO();
            BeanUtil.copyProperties(item, dto);
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BudgetItemDTO update(Long id, BudgetItemDTO dto) {
        BudgetItem entity = budgetItemRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("预算科目不存在: " + id));

        // P0 红线（见 BudgetControlServiceImpl 类注释）：已执行 / 冻结 / 剩余额 / 执行率
        // 是预算控制的**唯一事实来源**，只能经 BudgetControlServiceImpl 的冻结/释放/消耗变更。
        // 原实现用 copyProperties 全量覆盖 ⇒ 只要请求体带上这几个字段就能**直接篡改预算台账**，
        // 绕过全部预算控制。故此处把它们从拷贝中排除，随后按恒等式重算。
        // ⚠️ setIgnoreNullValue(true) 是必需的：Hutool 的 copyProperties **默认连 null 一起覆盖**，
        //    那样「只传部分字段」的更新会把未传字段（尤其 budgetAmount）清成 null，
        //    使随后的恒等式算出 0（2026-09-26 验证实测：只传 usedAmount 时 remaining 变成 0）。
        //    Hutool 无 (source,target,CopyOptions,ignoreProperties...) 重载，忽略列须写进 CopyOptions。
        CopyOptions options = CopyOptions.create()
                .setIgnoreNullValue(true)
                .setIgnoreProperties("id", "budgetId",
                        "usedAmount", "frozenAmount", "remainingAmount", "executionRate");
        BeanUtil.copyProperties(dto, entity, options);

        // 预算额若被调整，同步重算剩余额与执行率：
        //   remaining_amount = budget_amount − used_amount − frozen_amount
        //   execution_rate   = used_amount ÷ budget_amount × 100（budget 为 0 时置 0）
        // 口径与 BudgetControlServiceImpl#applyItemAmounts 一致，避免两处算法漂移。
        BigDecimal budget = nvl(entity.getBudgetAmount());
        BigDecimal used = nvl(entity.getUsedAmount());
        BigDecimal frozen = nvl(entity.getFrozenAmount());
        entity.setRemainingAmount(budget.subtract(used).subtract(frozen));
        entity.setExecutionRate(budget.compareTo(BigDecimal.ZERO) > 0
                ? used.multiply(BigDecimal.valueOf(100)).divide(budget, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        entity = budgetItemRepository.save(entity);

        BudgetItemDTO result = new BudgetItemDTO();
        BeanUtil.copyProperties(entity, result);
        return result;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    @Override
    public BudgetItemDTO getById(Long id) {
        BudgetItem entity = budgetItemRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("预算科目不存在: " + id));
        BudgetItemDTO dto = new BudgetItemDTO();
        BeanUtil.copyProperties(entity, dto);
        return dto;
    }
}
