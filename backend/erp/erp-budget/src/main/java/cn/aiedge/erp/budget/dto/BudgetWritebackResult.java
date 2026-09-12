package cn.aiedge.erp.budget.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 业务单据回写预算执行结果
 *
 * <p>回写是「尽力而为」的旁路动作：单据本身已完成记账，预算回写失败（如无匹配预算）
 * 不得反向阻断记账，但必须把未匹配 / 超支情况如实回报，供超支预警与人工核对。</p>
 */
@Data
public class BudgetWritebackResult {

    /** 成功回写的科目数 */
    private int matchedCount;

    /** 未匹配到预算的科目数（未接通预算） */
    private int skippedCount;

    /** 触发超支的科目数 */
    private int overBudgetCount;

    /** 实际回写金额合计 */
    private BigDecimal totalConsumed = BigDecimal.ZERO;

    /** 处理详情：未匹配、超支等说明 */
    private List<String> messages = new ArrayList<>();

    public void addMessage(String message) {
        this.messages.add(message);
    }
}
