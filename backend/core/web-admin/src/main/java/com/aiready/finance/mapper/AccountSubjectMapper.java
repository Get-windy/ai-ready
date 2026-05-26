package com.aiready.finance.mapper;

import com.aiready.finance.entity.AccountSubject;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 会计科目Mapper接口
 */
@Mapper
public interface AccountSubjectMapper extends BaseMapper<AccountSubject> {
    
    /**
     * 根据科目编码查询科目
     */
    @Select("SELECT * FROM fin_account_subject WHERE subject_code = #{subjectCode} AND deleted = 0")
    AccountSubject selectBySubjectCode(@Param("subjectCode") String subjectCode);
    
    /**
     * 检查科目编码是否存在
     */
    @Select("SELECT COUNT(*) FROM fin_account_subject WHERE subject_code = #{subjectCode} AND deleted = 0 AND id != #{excludeId}")
    Integer checkSubjectCodeExists(@Param("subjectCode") String subjectCode, @Param("excludeId") Long excludeId);
    
    /**
     * 根据科目类别查询科目列表
     */
    @Select("SELECT * FROM fin_account_subject WHERE subject_type = #{subjectType} AND deleted = 0 ORDER BY subject_code")
    List<AccountSubject> selectBySubjectType(@Param("subjectType") Integer subjectType);
    
    /**
     * 根据上级科目ID查询子科目
     */
    @Select("SELECT * FROM fin_account_subject WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY sort_order, subject_code")
    List<AccountSubject> selectByParentId(@Param("parentId") Long parentId);
    
    /**
     * 查询所有明细科目
     */
    @Select("SELECT * FROM fin_account_subject WHERE is_detail = 1 AND status = 1 AND deleted = 0 ORDER BY subject_code")
    List<AccountSubject> selectDetailSubjects();
    
    /**
     * 获取最大排序号
     */
    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM fin_account_subject WHERE deleted = 0")
    Integer selectMaxSortOrder();
}
