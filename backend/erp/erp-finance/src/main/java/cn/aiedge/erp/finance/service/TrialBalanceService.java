package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.TrialBalancePageDTO;
import cn.aiedge.erp.finance.dto.TrialBalanceQuery;

/**
 * 科目余额表 Service
 *
 * 按科目汇总期初/本期发生/本年累计/期末四段余额的试算平衡表（账簿—报表类）。
 */
public interface TrialBalanceService {

    /**
     * 查询科目余额表（无分页，返回科目行 + 合计行）
     */
    TrialBalancePageDTO queryTrialBalance(TrialBalanceQuery query);
}
