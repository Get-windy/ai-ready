package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.AuxBalancePageDTO;
import cn.aiedge.erp.finance.dto.AuxBalanceQuery;

/**
 * 辅助核算余额Service接口（辅助核算余额表）
 *
 * 口径（P0 红线）：只读取自凭证分录，严禁绕过凭证直改核算项余额。
 */
public interface FinanceAuxiliaryBalanceService {

    /**
     * 辅助核算余额表分页查询（按 科目 + 核算项 汇总四段余额）
     */
    AuxBalancePageDTO page(AuxBalanceQuery query);
}
