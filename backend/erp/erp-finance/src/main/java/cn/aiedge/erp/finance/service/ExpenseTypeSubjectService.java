package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.AccountSubjectDTO;
import cn.aiedge.erp.finance.dto.AccountSubjectQuery;
import cn.aiedge.erp.finance.dto.FinanceAuxiliaryTypeDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 费用类型（资料 → 财务账户 → 费用类型）服务。
 *
 * <p>本页是「费用类会计科目」视图，非独立主数据：口径 = 会计科目中 subject_type=5（损益类）
 * 且 direction=1（借方），直接读写 {@code finance_account_subject}，严禁另建费用类型字典表。</p>
 *
 * <p>对标依据（ql361 实测）：费用类型页与会计科目页调用同一接口
 * {@code cc.erp.bll.bas.account.getlist}，仅 {@code bastype} 不同（费用类型=fee/root=00004，
 * 会计科目=account/root=00000）；行内「修改」打开的编辑器标题为「会计科目」，
 * 字段 科目编号/科目名称/助记码/科目全名/核算项/借-贷 与会计科目完全一致。</p>
 */
public interface ExpenseTypeSubjectService {

    /** 费用类科目分页（内存分页，费用科目为个位数量级） */
    Page<AccountSubjectDTO> page(AccountSubjectQuery query, long pageNum, long pageSize);

    /** 费用类科目平铺列表（导出/下拉共用同一口径） */
    List<AccountSubjectDTO> list(AccountSubjectQuery query);

    /** 详情（非费用类科目视为不存在） */
    AccountSubjectDTO getById(Long id);

    /** 新增费用科目（强制损益类，方向缺省借方） */
    AccountSubjectDTO create(AccountSubjectDTO dto);

    /** 修改费用科目 */
    AccountSubjectDTO update(Long id, AccountSubjectDTO dto);

    /** 删除费用科目（沿用会计科目约束：有下级或被凭证引用时不可删） */
    void delete(Long id);

    /** 启用/停用 */
    AccountSubjectDTO toggleEnabled(Long id, boolean enabled);

    /** 核算项下拉（复用 finance_auxiliary_type 主数据） */
    List<FinanceAuxiliaryTypeDTO> getAuxTypeOptions();
}
