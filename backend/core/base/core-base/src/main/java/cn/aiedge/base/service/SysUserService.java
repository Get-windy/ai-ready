package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysTenant;
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
     * 校验用户名密码（不含租户归属），返回用户
     * <p>
     * 登录第一步用：用户名全局唯一，凭用户名+密码先确认身份；
     * 企业由「该用户所属的企业列表」推导，故此处不校验租户。
     * </p>
     *
     * @param username 用户名
     * @param password 密码
     * @return 用户实体
     */
    SysUser verifyCredentials(String username, String password);

    /**
     * 按手机号定位登录账号（全局、跨租户），供短信验证码登录使用
     * <p>
     * {@code sys_user.phone} 没有唯一约束：命中多个账号时**明确报错**让用户改用用户名登录，
     * 而不是随机取一个（那会把人登进别人的账号）。
     * </p>
     *
     * @param phone 手机号
     * @return 用户实体
     */
    SysUser findByPhoneForLogin(String phone);

    /**
     * 完成登录：校验用户确实属于该企业后签发 Token，并写入会话租户
     *
     * @param user     已通过 {@link #verifyCredentials} 校验的用户
     * @param tenantId 目标企业
     * @param loginIp  登录IP
     * @return 登录Token
     */
    String completeLogin(SysUser user, Long tenantId, String loginIp);

    /**
     * 记录「上次登录该企业」的时间
     * <p>登录成功与切换企业后调用；多企业用户下次登录时据此把该企业排在候选第一位。</p>
     *
     * @param userId   用户ID
     * @param tenantId 企业ID
     */
    void touchTenantLoginTime(Long userId, Long tenantId);

    /**
     * 该用户上次登录的企业ID；从未登录过返回 null
     *
     * @param userId 用户ID
     * @return 企业ID 或 null
     */
    Long getLastLoginTenantId(Long userId);

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
     * 获取用户可访问的租户列表
     */
    List<SysTenant> getUserTenants(Long userId);

    /**
     * 用户是否属于指定租户
     */
    boolean isUserInTenant(Long userId, Long tenantId);

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
     * 批量分配角色
     *
     * @param userIds 用户ID列表
     * @param roleIds 角色ID列表
     */
    void batchAssignRoles(List<Long> userIds, List<Long> roleIds);

    /**
     * 启用/禁用用户
     */
    void updateUserStatus(Long userId, Integer status);
}