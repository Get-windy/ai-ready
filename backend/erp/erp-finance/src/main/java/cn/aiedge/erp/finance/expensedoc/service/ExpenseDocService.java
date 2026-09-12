package cn.aiedge.erp.finance.expensedoc.service;

import cn.aiedge.erp.finance.expensedoc.dto.ExpenseDocItemVO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseDocQuery;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseDocSaveDTO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseDocVO;
import cn.aiedge.erp.finance.expensedoc.entity.ExpenseDoc;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 费用单 Service接口
 * 非主营支出费用登记：往来单位费用/内部费用记账生成凭证（KJPZ-）。
 */
public interface ExpenseDocService extends IService<ExpenseDoc> {

    /** 多条件分页查询(按单据) */
    Page<ExpenseDoc> pageQuery(ExpenseDocQuery query);

    /** 多条件分页查询(按明细) */
    Page<ExpenseDocItemVO> pageDetail(ExpenseDocQuery query);

    /** 生成下一费用单号：YBFYD-YYYYMMDD-序号 */
    String generateDocNo();

    /** 详情（含费用项明细） */
    ExpenseDocVO getDetail(Long id);

    /** 保存草稿（含费用项明细） */
    ExpenseDoc saveDraft(ExpenseDocSaveDTO dto);

    /** 更新草稿（明细整体替换） */
    ExpenseDoc update(ExpenseDocSaveDTO dto);

    /** 记账（生成凭证 + 动付款账户余额 + 记资金流水） */
    ExpenseDoc confirm(Long id, Long operatorId, String operatorName);

    /** 取消单据（仅草稿） */
    void cancel(Long id);

    /** 删除单据（仅草稿/已取消） */
    void remove(Long id);
}
