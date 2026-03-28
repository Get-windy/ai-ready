package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 角色服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 创建角色
     */
    Long createRole(SysRole role);

    /**
     * 更新角色
     */
    void updateRole(SysRole role);

    /**
     * 删除角色
     */
    void deleteRole(Long roleId);

    /**
     * 批量删除角色
     */
    void batchDeleteRoles(List<Long> roleIds);

    /**
     * 分页查询角色
     */
    Page<SysRole> pageRoles(Page<SysRole> page, Long tenantId,
                            String roleName, Integer status);

    /**
     * 获取角色详情
     */
    SysRole getRoleDetail(Long roleId);

    /**
     * 获取所有角色（下拉选择用）
     */
    List<SysRole> listAllRoles(Long tenantId);

    /**
     * 分配菜单权限
     */
    void assignMenus(Long roleId, List<Long> menuIds);

    /**
     * 分配API权限
     */
    void assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 获取角色的菜单ID列表
     */
    List<Long> getRoleMenuIds(Long roleId);

    /**
     * 获取角色的权限ID列表
     */
    List<Long> getRolePermissionIds(Long roleId);

    /**
     * 启用/禁用角色
     */
    void updateRoleStatus(Long roleId, Integer status);

    /**
     * 检查角色编码是否存在
     */
    boolean checkRoleCodeExists(String roleCode, Long tenantId, Long excludeId);
}