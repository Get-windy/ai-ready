package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.AccountSubjectDTO;

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
     * 获取科目树形结构
     */
    List<AccountSubjectDTO> getTree();

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
}
