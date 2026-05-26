package cn.aiedge.base.service;

import cn.aiedge.base.entity.RoleInheritance;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 角色继承关系服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface RoleInheritanceService extends IService<RoleInheritance> {

    /**
     * 设置角色继承关系
     */
    void setRoleInheritance(Long parentRoleId, Long childRoleId, Integer inheritanceType);

    /**
     * 批量设置角色继承关系
     */
    void batchSetRoleInheritance(Long parentRoleId, List<Long> childRoleIds, Integer inheritanceType);

    /**
     * 删除角色继承关系
     */
    void removeRoleInheritance(Long parentRoleId, Long childRoleId);

    /**
     * 获取角色的所有父角色
     */
    List<Long> getParentRoleIds(Long childRoleId);

    /**
     * 获取角色的所有子角色
     */
    List<Long> getChildRoleIds(Long parentRoleId);

    /**
     * 获取角色继承的所有权限（包括继承的权限）
     */
    List<Long> getAllRolePermissionsWithInheritance(Long roleId);
}