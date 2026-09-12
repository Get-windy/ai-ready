package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.AccountSubjectDTO;
import cn.aiedge.erp.finance.dto.AccountSubjectQuery;
import cn.aiedge.erp.finance.dto.FinanceAuxiliaryTypeDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 其他收入（资料 → 财务账户 → 其他收入）服务。
 *
 * <p>本页是「收入类会计科目」视图，非独立主数据：口径 = 会计科目中 subject_type=5（损益类）
 * 且 direction=2（贷方），直接读写 {@code finance_account_subject}，严禁另建收入类型字典。</p>
 */
public interface OtherIncomeSubjectService {

    /** 收入类科目分页（内存分页，收入科目为个位数量级） */
    Page<AccountSubjectDTO> page(AccountSubjectQuery query, long pageNum, long pageSize);

    /** 收入类科目平铺列表（导出/下拉共用同一口径） */
    List<AccountSubjectDTO> list(AccountSubjectQuery query);

    /** 详情（非收入类科目视为不存在） */
    AccountSubjectDTO getById(Long id);

    /** 新增收入科目（强制损益类，方向缺省贷方） */
    AccountSubjectDTO create(AccountSubjectDTO dto);

    /** 修改收入科目 */
    AccountSubjectDTO update(Long id, AccountSubjectDTO dto);

    /** 删除收入科目（沿用会计科目约束：有下级或被凭证引用时不可删） */
    void delete(Long id);

    /** 启用/停用 */
    AccountSubjectDTO toggleEnabled(Long id, boolean enabled);

    /** 核算项下拉（复用 finance_auxiliary_type 主数据） */
    List<FinanceAuxiliaryTypeDTO> getAuxTypeOptions();
}
