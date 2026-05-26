package com.aiready.permission.mapper;

import com.aiready.permission.entity.Organization;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 组织机构Mapper接口
 */
public interface OrganizationMapper extends BaseMapper<Organization> {

    /**
     * 根据用户ID查询部门ID
     */
    @Select("SELECT dept_id FROM sys_user WHERE id = #{userId}")
    Long selectDeptIdByUserId(@Param("userId") Long userId);

    /**
     * 根据父ID查询子部门列表
     */
    @Select("SELECT * FROM sys_organization WHERE parent_id = #{parentId} AND status = 1 AND deleted = 0")
    List<Organization> selectChildrenByParentId(@Param("parentId") Long parentId);
}
