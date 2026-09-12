package cn.aiedge.erp.budget.service;

import cn.aiedge.erp.budget.dto.BudgetWritebackRequest;
import cn.aiedge.erp.budget.dto.BudgetWritebackResult;

/**
 * 业务单据回写预算执行（P1 执行回写）
 *
 * <p>费用、支出类单据记账后调用，将金额按费用科目计入《预算执行》的「已执行」。
 * 这是预算执行「只读取自预算计划 + 已记账支出」中「已记账支出」的唯一来源，
 * 页面与其他服务均不得直接改写已执行 / 剩余额。</p>
 */
public interface BudgetWritebackService {

    /**
     * 按单据金额明细回写预算执行。
     *
     * @param request 回写请求（来源单据 + 年度 + 部门 + 金额明细）
     * @return 回写结果（匹配数 / 未匹配数 / 超支数 / 说明）
     */
    BudgetWritebackResult consume(BudgetWritebackRequest request);
}
