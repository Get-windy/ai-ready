package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.AccountSubjectDTO;
import cn.aiedge.erp.finance.dto.AccountSubjectQuery;
import cn.aiedge.erp.finance.dto.FinanceAuxiliaryTypeDTO;

import java.util.List;

/**
 * 会计科目Service接口
 */
public interface AccountSubjectService {

    /**
     * 获取所有会计科目
     */
    List<AccountSubjectDTO> getAll();

    /**
     * 按条件查询科目（扁平列表，含上级科目编码/名称与核算项名称）
     */
    List<AccountSubjectDTO> search(AccountSubjectQuery query);

    /**
     * 根据ID获取会计科目
     */
    AccountSubjectDTO getById(Long id);

    /**
     * 创建会计科目
     */
    AccountSubjectDTO create(AccountSubjectDTO dto);

    /**
     * 更新会计科目
     */
    AccountSubjectDTO update(Long id, AccountSubjectDTO dto);

    /**
     * 删除会计科目
     */
    void delete(Long id);

    /**
     * 获取科目树形结构（全量，含停用）
     */
    List<AccountSubjectDTO> getTree();

    /**
     * 按条件获取科目结构：hierarchical=true 返回树形，false 返回平铺列表
     */
    List<AccountSubjectDTO> getTree(AccountSubjectQuery query);

    /**
     * 根据科目类型查询
     */
    List<AccountSubjectDTO> getByType(Integer subjectType);

    /**
     * 启用/禁用科目
     */
    AccountSubjectDTO enable(Long id, boolean enabled);

    /**
     * 批量删除会计科目
     */
    void deleteBatch(List<Long> ids);

    /**
     * 收集指定科目及其全部下级科目的科目编码（账簿/余额表统一科目范围口径）
     *
     * @param subjectId 科目ID；为空或科目不存在时返回空列表（表示不限制科目范围）
     */
    List<String> collectCodeWithDescendants(Long subjectId);

    /**
     * 核算项可选项：finance_auxiliary_type（辅助核算类型）启用项
     */
    List<FinanceAuxiliaryTypeDTO> getAuxTypeOptions();
}
