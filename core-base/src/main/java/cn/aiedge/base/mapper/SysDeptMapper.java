package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysDept;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 部门Mapper
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 获取部门树
     */
    @Select("SELECT * FROM sys_dept WHERE tenant_id = #{tenantId} AND deleted = 0 ORDER BY sort")
    List<SysDept> selectDeptTree(@Param("tenantId") Long tenantId);

    /**
     * 获取子部门
     */
    @Select("SELECT * FROM sys_dept WHERE parent_id = #{parentId} AND tenant_id = #{tenantId} AND deleted = 0 ORDER BY sort")
    List<SysDept> selectChildrenByParentId(@Param("parentId") Long parentId, @Param("tenantId") Long tenantId);

    /**
     * 检查部门编码是否存在
     */
    @Select("SELECT COUNT(*) FROM sys_dept WHERE dept_code = #{deptCode} AND tenant_id = #{tenantId} AND deleted = 0 AND (#{excludeId} IS NULL OR id != #{excludeId})")
    int checkDeptCodeExists(@Param("deptCode") String deptCode, @Param("tenantId") Long tenantId, @Param("excludeId") Long excludeId);

    /**
     * 获取部门下的用户数量
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE dept_id = #{deptId} AND deleted = 0")
    int countUsersByDeptId(@Param("deptId") Long deptId);
}