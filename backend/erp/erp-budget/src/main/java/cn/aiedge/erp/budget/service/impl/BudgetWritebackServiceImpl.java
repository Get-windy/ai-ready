package cn.aiedge.erp.budget.service.impl;

import cn.aiedge.erp.budget.dto.BudgetWritebackItem;
import cn.aiedge.erp.budget.dto.BudgetWritebackRequest;
import cn.aiedge.erp.budget.dto.BudgetWritebackResult;
import cn.aiedge.erp.budget.model.BudgetItem;
import cn.aiedge.erp.budget.repository.BudgetItemRepository;
import cn.aiedge.erp.budget.service.BudgetControlService;
import cn.aiedge.erp.budget.service.BudgetWritebackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 预算执行回写实现：按费用科目匹配预算科目并计入已执行。
 *
 * <p><b>匹配规则</b>：会计年度 + 部门（请求带部门时限定同部门）+ 费用科目编码；
 * 仅在预算已审批 / 执行中时回写（草稿、待审批、已驳回不参与执行）。</p>
 *
 * <p><b>超支处理</b>：单据已记账，回写不得反向阻断，故超支不抛异常，
 * 而是照常计入已执行并由《预算执行》超支预警呈现（P1 超支预警辅助控制）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetWritebackServiceImpl implements BudgetWritebackService {

    private final BudgetItemRepository budgetItemRepository;
    private final BudgetControlService budgetControlService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BudgetWritebackResult consume(BudgetWritebackRequest request) {
        BudgetWritebackResult result = new BudgetWritebackResult();
        if (request == null || request.getFiscalYear() == null || request.getItems() == null
                || request.getItems().isEmpty()) {
            return result;
        }

        for (BudgetWritebackItem line : request.getItems()) {
            BigDecimal amount = line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO;
            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            String subjectCode = line.getSubjectCode();
            if (subjectCode == null || subjectCode.isBlank()) {
                result.setSkippedCount(result.getSkippedCount() + 1);
                result.addMessage("费用项【" + safeName(line) + "】无费用科目编码，未接通预算");
                continue;
            }

            List<BudgetItem> candidates = budgetItemRepository.findConsumableBySubject(
                    request.getFiscalYear(), request.getDepartmentId(), subjectCode);
            if (candidates.isEmpty()) {
                result.setSkippedCount(result.getSkippedCount() + 1);
                result.addMessage("科目 " + subjectCode + " 未匹配到可执行预算，未回写");
                continue;
            }
            if (candidates.size() > 1) {
                result.addMessage("科目 " + subjectCode + " 匹配到 " + candidates.size()
                        + " 条预算，回写优先取执行中预算 #" + candidates.get(0).getBudgetId());
            }

            BudgetItem target = candidates.get(0);
            Map<String, Object> consumed = budgetControlService.consumeBudget(
                    target.getBudgetId(), target.getId(), amount,
                    request.getSourceType(), request.getSourceNo(), request.getSourceId(),
                    buildDescription(request, line), true);

            result.setMatchedCount(result.getMatchedCount() + 1);
            result.setTotalConsumed(result.getTotalConsumed().add(amount));
            if (Boolean.TRUE.equals(consumed.get("overBudget"))) {
                result.setOverBudgetCount(result.getOverBudgetCount() + 1);
                result.addMessage("科目 " + subjectCode + " 执行进度 " + consumed.get("executionRate")
                        + "%，已超支（剩余 " + consumed.get("remainingAmount") + "）");
            }
        }

        if (result.getMatchedCount() > 0) {
            log.info("预算执行回写：来源 {} {}，匹配 {} 项，金额 {}，超支 {} 项",
                    request.getSourceType(), request.getSourceNo(),
                    result.getMatchedCount(), result.getTotalConsumed(), result.getOverBudgetCount());
        }
        return result;
    }

    private String buildDescription(BudgetWritebackRequest request, BudgetWritebackItem line) {
        String source = request.getSourceNo() != null ? request.getSourceNo() : String.valueOf(request.getSourceId());
        return "单据回写 " + source + " 科目 " + line.getSubjectCode();
    }

    private String safeName(BudgetWritebackItem line) {
        return line.getSubjectName() != null ? line.getSubjectName() : String.valueOf(line.getSubjectCode());
    }
}
