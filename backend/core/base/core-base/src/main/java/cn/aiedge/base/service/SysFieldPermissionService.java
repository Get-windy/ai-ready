package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysFieldPermission;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 字段级权限服务
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface SysFieldPermissionService extends IService<SysFieldPermission> {

    /**
     * 根据角色ID列表查询字段权限
     */
    List<SysFieldPermission> getByRoleIds(List<Long> roleIds);

    /**
     * 根据角色ID列表和目标表名查询字段权限
     */
    List<SysFieldPermission> getByRoleAndTable(List<Long> roleIds, String tableName);

    /**
     * 保存角色的字段权限（先删后增）
     */
    void saveRoleFieldPermissions(Long roleId, List<SysFieldPermission> permissions);
}
