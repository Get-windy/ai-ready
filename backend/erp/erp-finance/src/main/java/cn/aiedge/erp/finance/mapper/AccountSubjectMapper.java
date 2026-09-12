package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.model.entity.AccountSubject;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 会计科目Mapper接口
 */
@Mapper
public interface AccountSubjectMapper extends BaseMapper<AccountSubject> {

    /**
     * 根据父级科目ID查询
     */
    @Select("SELECT * FROM finance_account_subject WHERE parent_id = #{parentId} AND deleted_flag = 0")
    List<AccountSubject> findByParentId(Long parentId);

    /**
     * 根据科目编码查询
     */
    @Select("SELECT * FROM finance_account_subject WHERE subject_code = #{subjectCode} AND deleted_flag = 0")
    Optional<AccountSubject> findBySubjectCode(String subjectCode);

    /**
     * 根据科目类型查询
     */
    @Select("SELECT * FROM finance_account_subject WHERE subject_type = #{subjectType} AND deleted_flag = 0")
    List<AccountSubject> findBySubjectType(Integer subjectType);

    /**
     * 根据科目层级查询
     */
    @Select("SELECT * FROM finance_account_subject WHERE level = #{level} AND deleted_flag = 0")
    List<AccountSubject> findByLevel(Integer level);

    /**
     * 根据启用状态查询
     */
    @Select("SELECT * FROM finance_account_subject WHERE is_enabled = #{isEnabled} AND deleted_flag = 0")
    List<AccountSubject> findByIsEnabled(Boolean isEnabled);

    /**
     * 核算项可选项：读 finance_auxiliary_type（辅助核算类型）主数据，不另建字典。
     * 该表逻辑删除列名为 deleted_flag（与 BaseEntity 一致），此处显式列名直读，
     * 避免经实体映射产生列名不匹配错误（历史曾误写 deleted 导致 /aux-types 恒 500）。
     */
    @Select("SELECT id, type_code AS typeCode, type_name AS typeName FROM finance_auxiliary_type "
            + "WHERE deleted_flag = 0 AND enabled = true ORDER BY sort ASC, id ASC")
    List<Map<String, Object>> selectAuxTypeOptions();
}
