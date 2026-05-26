package com.aiready.permission.mapper;

import com.aiready.permission.entity.UserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户角色关联Mapper接口
 */
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 批量插入
     */
    void insertBatch(@Param("list") List<UserRole> userRoles);
}
