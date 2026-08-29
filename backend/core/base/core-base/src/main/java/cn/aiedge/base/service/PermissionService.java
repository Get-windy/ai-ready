package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysPermission;
import cn.aiedge.common.dto.permission.PermissionCreateRequest;
import cn.aiedge.common.dto.permission.PermissionDetailVO;
import cn.aiedge.common.dto.permission.PermissionQueryRequest;
import cn.aiedge.common.dto.permission.PermissionUpdateRequest;
import cn.aiedge.common.result.PageResult;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 权限服务接口
 */
public interface PermissionService extends IService<SysPermission> {

    /**
     * 分页查询权限
     */
    PageResult<PermissionDetailVO> pageList(PermissionQueryRequest request);

    /**
     * 获取权限树形结构
     */
    List<PermissionDetailVO> getTree();

    /**
     * 获取用户菜单权限树
     */
    List<PermissionDetailVO> getUserMenuTree(Long userId);

    /**
     * 根据ID获取权限详情
     */
    PermissionDetailVO getDetail(Long id);

    /**
     * 创建权限
     */
    Long create(PermissionCreateRequest request);

    /**
     * 更新权限
     */
    void update(PermissionUpdateRequest request);

    /**
     * 删除权限
     */
    void delete(Long id);

    /**
     * 批量删除权限
     */
    void batchDelete(List<Long> ids);

    /**
     * 启用/禁用权限
     */
    void updateStatus(Long id, Integer status);

    /**
     * 根据权限编码查询权限
     */
    SysPermission getByPermissionCode(String permissionCode);

    /**
     * 根据用户ID查询权限列表
     */
    List<SysPermission> getByUserId(Long userId);

    /**
     * 根据角色ID查询权限列表
     */
    List<SysPermission> getByRoleId(Long roleId);

    /**
     * 获取子权限列表
     */
    List<SysPermission> getByParentId(Long parentId);
}
