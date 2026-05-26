package com.aiready.permission.mapper;

import com.aiready.permission.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色Mapper接口
 */
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据用户ID查询角色列表
     */
    @Select("SELECT r.* FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0 " +
            "ORDER BY r.sort_order")
    List<Role> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 批量插入用户角色
     */
    void insertBatch(@Param("list") List<com.aiready.permission.entity.UserRole> userRoles);
}
