package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.AccountSubject;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AccountSubjectMapper extends BaseMapper<AccountSubject> {
    
    @Select("SELECT * FROM finance_account_subject WHERE tenant_id = #{tenantId} AND deleted = 0 AND enabled = 1 ORDER BY subject_code")
    List<AccountSubject> listAllEnabled(@Param("tenantId") Long tenantId);
    
    @Select("SELECT * FROM finance_account_subject WHERE tenant_id = #{tenantId} AND deleted = 0 AND parent_id = #{parentId} ORDER BY subject_code")
    List<AccountSubject> listByParentId(@Param("tenantId") Long tenantId, @Param("parentId") Long parentId);
    
    @Select("SELECT * FROM finance_account_subject WHERE tenant_id = #{tenantId} AND deleted = 0 AND subject_type = #{subjectType} ORDER BY subject_code")
    List<AccountSubject> listByType(@Param("tenantId") Long tenantId, @Param("subjectType") Integer subjectType);
    
    @Select("SELECT * FROM finance_account_subject WHERE tenant_id = #{tenantId} AND deleted = 0 AND leaf_flag = 1 ORDER BY subject_code")
    List<AccountSubject> listLeafSubjects(@Param("tenantId") Long tenantId);
    
    @Select("SELECT * FROM finance_account_subject WHERE tenant_id = #{tenantId} AND deleted = 0 AND subject_code = #{subjectCode}")
    AccountSubject getByCode(@Param("tenantId") Long tenantId, @Param("subjectCode") String subjectCode);
    
    @Select("SELECT COUNT(*) FROM finance_account_subject WHERE tenant_id = #{tenantId} AND deleted = 0 AND parent_id = #{parentId}")
    int countChildren(@Param("tenantId") Long tenantId, @Param("parentId") Long parentId);
}