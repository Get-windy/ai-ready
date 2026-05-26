package cn.aiedge.base.service;

import cn.aiedge.base.entity.DataPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 数据权限服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface DataPermissionService extends IService<DataPermission> {

    /**
     * 创建数据权限
     */
    Long createDataPermission(DataPermission dataPermission);

    /**
     * 更新数据权限
     */
    void updateDataPermission(DataPermission dataPermission);

    /**
     * 删除数据权限
     */
    void deleteDataPermission(Long dataPermissionId);

    /**
     * 分页查询数据权限
     */
    Page<DataPermission> pageDataPermissions(Page<DataPermission> page, Long tenantId, 
                                             String permissionName, Integer scopeType, Integer status);

    /**
     * 获取用户的数据权限
     */
    List<DataPermission> getUserDataPermissions(Long userId);

    /**
     * 获取角色的数据权限
     */
    List<DataPermission> getRoleDataPermissions(Long roleId);

    /**
     * 检查用户是否有数据访问权限
     */
    boolean checkUserDataPermission(Long userId, String resourceType, String resourceId, String action);

    /**
     * 批量分配数据权限给用户
     */
    void batchAssignDataPermissionToUser(Long userId, List<Long> dataPermissionIds);

    /**
     * 批量分配数据权限给角色
     */
    void batchAssignDataPermissionToRole(Long roleId, List<Long> dataPermissionIds);

    /**
     * 更新数据权限状态
     */
    void updateDataPermissionStatus(Long dataPermissionId, Integer status);
}