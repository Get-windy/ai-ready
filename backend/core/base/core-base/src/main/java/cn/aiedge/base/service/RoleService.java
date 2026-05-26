package cn.aiedge.base.service;

import cn.aiedge.base.entity.Role;
import cn.aiedge.common.dto.role.RoleCreateRequest;
import cn.aiedge.common.dto.role.RoleDetailVO;
import cn.aiedge.common.dto.role.RoleQueryRequest;
import cn.aiedge.common.dto.role.RoleUpdateRequest;
import cn.aiedge.common.result.PageResult;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 角色服务接口
 */
public interface RoleService extends IService<Role> {

    /**
     * 分页查询角色
     */
    PageResult<RoleDetailVO> pageList(RoleQueryRequest request);

    /**
     * 获取所有角色列表（下拉选择用）
     */
    List<RoleDetailVO> listAll();

    /**
     * 根据ID获取角色详情
     */
    RoleDetailVO getDetail(Long id);

    /**
     * 创建角色
     */
    Long create(RoleCreateRequest request);

    /**
     * 更新角色
     */
    void update(RoleUpdateRequest request);

    /**
     * 删除角色
     */
    void delete(Long id);

    /**
     * 批量删除角色
     */
    void batchDelete(List<Long> ids);

    /**
     * 启用/禁用角色
     */
    void updateStatus(Long id, Integer status);

    /**
     * 分配权限
     */
    void assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 根据角色编码查询角色
     */
    Role getByRoleCode(String roleCode);

    /**
     * 根据用户ID查询角色列表
     */
    List<Role> getByUserId(Long userId);

    /**
     * 获取角色的权限ID列表
     */
    List<Long> getPermissionIds(Long roleId);
}
