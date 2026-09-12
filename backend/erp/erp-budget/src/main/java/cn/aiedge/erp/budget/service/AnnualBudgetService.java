package cn.aiedge.erp.budget.service;

import cn.aiedge.erp.budget.dto.AnnualBudgetDTO;
import cn.aiedge.erp.budget.dto.AnnualBudgetQuery;

import java.util.List;
import java.util.Map;

public interface AnnualBudgetService {

    AnnualBudgetDTO create(AnnualBudgetDTO dto);

    AnnualBudgetDTO update(Long id, AnnualBudgetDTO dto);

    void delete(Long id);

    AnnualBudgetDTO getById(Long id);

    AnnualBudgetDTO submit(Long id);

    AnnualBudgetDTO approve(Long id);

    AnnualBudgetDTO reject(Long id);

    AnnualBudgetDTO startExec(Long id);

    AnnualBudgetDTO close(Long id);

    List<AnnualBudgetDTO> exportList(String keyword, Integer fiscalYear, String departmentId, String status);

    /**
     * 按预算科目明细重算年度预算汇总（预算金额/已执行/冻结/剩余/执行率）。
     * 预算控制（冻结/释放/消耗）后必须调用，保证「剩余 = 预算 − 已执行 − 冻结」在汇总层同样成立。
     *
     * @param budgetId 预算ID
     */
    void recalculateSummary(Long budgetId);

    // ═══ 金标准编制能力 ═══

    /** 保存（新增或更新，含预算科目明细），返回详情 */
    AnnualBudgetDTO save(AnnualBudgetDTO dto);

    /** 生成下一预算编制单号（YSD-yyyyMMdd-001） */
    String generateDocNo();

    /** 多条件分页查询 */
    Map<String, Object> pageQuery(AnnualBudgetQuery query);

    /** 批量删除（仅草稿/已驳回） */
    int batchDelete(List<Long> ids);

    /** 批量提交审批（仅草稿/已驳回） */
    int batchSubmit(List<Long> ids);

    /** 批量审批通过 */
    int batchApprove(List<Long> ids, Long auditorId, String auditorName, String auditRemark);

    /** 批量驳回 */
    int batchReject(List<Long> ids, Long auditorId, String auditorName, String auditRemark);

    /** 打印次数 +1 */
    void increasePrintCount(Long id);
}
