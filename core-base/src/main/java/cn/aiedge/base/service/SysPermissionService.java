package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 权限服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface SysPermissionService extends IService<SysPermission> {

    /**
     * 创建权限
     */
    Long createPermission(SysPermission permission);

    /**
     * 更新权限
     */
    void updatePermission(SysPermission permission);

    /**
     * 删除权限
     */
    void deletePermission(Long permissionId);

    /**
     * 批量删除权限
     */
    void batchDeletePermissions(List<Long> permissionIds);

    /**
     * 分页查询权限
     */
    Page<SysPermission> pagePermissions(Page<SysPermission> page, Long tenantId,
                                         String permissionName, Integer permissionType, Integer status);

    /**
     * 获取权限详情
     */
    SysPermission getPermissionDetail(Long permissionId);

    /**
     * 获取权限树
     */
    List<SysPermission> getPermissionTree(Long tenantId);

    /**
     * 获取用户权限列表
     */
    List<SysPermission> getUserPermissions(Long userId);

    /**
     * 获取角色权限列表
     */
    List<SysPermission> getRolePermissions(Long roleId);

    /**
     * 获取子权限列表
     */
    List<SysPermission> getChildrenPermissions(Long parentId, Long tenantId);

    /**
     * 检查权限编码是否存在
     */
    boolean checkPermissionCodeExists(String permissionCode, Long tenantId, Long excludeId);

    /**
     * 更新权限状态
     */
    void updatePermissionStatus(Long permissionId, Integer status);

    /**
     * 更新权限排序
     */
    void updatePermissionSort(Long permissionId, Integer sort);
}