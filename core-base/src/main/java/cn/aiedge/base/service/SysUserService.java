package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysUser;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 用户服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 用户登录
     * 
     * @param username 用户名
     * @param password 密码
     * @param tenantId 租户ID
     * @param loginIp 登录IP
     * @return 登录Token
     */
    String login(String username, String password, Long tenantId, String loginIp);

    /**
     * 用户登出
     */
    void logout();

    /**
     * 创建用户
     */
    Long createUser(SysUser user);

    /**
     * 更新用户
     */
    void updateUser(SysUser user);

    /**
     * 删除用户
     */
    void deleteUser(Long userId);

    /**
     * 批量删除用户
     */
    void batchDeleteUsers(List<Long> userIds);

    /**
     * 重置密码
     */
    void resetPassword(Long userId, String newPassword);

    /**
     * 修改密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 分页查询用户
     */
    Page<SysUser> pageUsers(Page<SysUser> page, Long tenantId, 
                            String username, Integer status, Long deptId);

    /**
     * 获取用户详情
     */
    SysUser getUserDetail(Long userId);

    /**
     * 获取用户角色编码列表
     */
    List<String> getUserRoleCodes(Long userId);

    /**
     * 获取用户权限编码列表
     */
    List<String> getUserPermissionCodes(Long userId);

    /**
     * 分配角色
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 启用/禁用用户
     */
    void updateUserStatus(Long userId, Integer status);
}