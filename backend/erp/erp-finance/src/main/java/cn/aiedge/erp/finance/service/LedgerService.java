package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.LedgerEntryDTO;
import cn.aiedge.erp.finance.model.entity.Voucher;

import java.util.List;
import java.util.Map;

/**
 * 分类账Service接口
 */
public interface LedgerService {

    /**
     * 获取指定科目和年度的分类账条目
     */
    List<LedgerEntryDTO> getLedger(Long subjectId, Integer fiscalYear);

    /**
     * 获取试算平衡表（返回条目列表 + 汇总信息）
     */
    Map<String, Object> getTrialBalance(Integer fiscalYear, Integer fiscalPeriod);

    /**
     * 过账到分类账（凭证过账后调用，更新所有分类账条目）
     */
    void postToLedger(Voucher voucher);

    /**
     * 期末结账（结转下期）
     */
    void closePeriod(Integer fiscalYear, Integer fiscalPeriod);
}
