package com.aiready.permission.mapper;

import com.aiready.permission.entity.RolePermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色权限关联Mapper接口
 */
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    /**
     * 批量插入
     */
    void insertBatch(@Param("list") List<RolePermission> rolePermissions);
}
