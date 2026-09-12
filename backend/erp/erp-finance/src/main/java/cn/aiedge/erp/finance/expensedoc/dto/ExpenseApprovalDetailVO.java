package cn.aiedge.erp.finance.expensedoc.dto;

import cn.aiedge.erp.finance.expensedoc.entity.ExpenseApproval;
import lombok.Data;

import java.util.List;

/**
 * 费用审批详情 VO：费用单（含费用项明细）+ 审批记录
 */
@Data
public class ExpenseApprovalDetailVO {

    /** 费用单详情（含 items 费用项明细） */
    private ExpenseDocVO doc;

    /** 审批记录（按时间正序） */
    private List<ExpenseApproval> records;
}
