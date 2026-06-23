package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.model.entity.AccountSubject;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
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
}
