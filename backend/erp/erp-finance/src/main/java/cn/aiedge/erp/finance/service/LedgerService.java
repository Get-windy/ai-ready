package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.GeneralLedgerQueryDTO;
import cn.aiedge.erp.finance.dto.GeneralLedgerRowDTO;
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
     * 总账账簿查询（按科目层级汇总，期初余额 + 本期发生 = 期末余额）
     *
     * @param query 会计月起止 / 科目层级 / 显示选项
     * @return 账簿行（每科目含期初余额行与本期发生行）
     */
    List<GeneralLedgerRowDTO> queryGeneralReport(GeneralLedgerQueryDTO query);

    /**
     * 总账科目层级选项（科目字典 level 去重升序）
     */
    List<Integer> listSubjectLevels();

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
