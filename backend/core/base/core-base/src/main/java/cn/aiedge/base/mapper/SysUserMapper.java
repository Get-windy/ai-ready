package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户Mapper
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名查询用户（忽略租户拦截器，因为用户名全局唯一）
     */
    @InterceptorIgnore(tenantLine = "true")
    SysUser selectByUsername(@Param("username") String username, @Param("tenantId") Long tenantId);

    /**
     * 根据手机号查询用户
     */
    SysUser selectByPhone(@Param("phone") String phone, @Param("tenantId") Long tenantId);

    /**
     * 按手机号查询用户列表（登录专用：全局、跨租户）
     * <p>
     * 与 {@link #selectByUsername} 同口径 —— 登录发生在认证之前，此时尚未确定企业，
     * 必须全局查。显式忽略租户拦截器，不依赖"租户不可解析时自动跳过"这一隐式行为。
     * </p>
     * <p>
     * 返回 List 而非单个：{@code sys_user.phone} 目前**没有唯一约束**，
     * 手机号可能命中多个账号，调用方必须显式处理这种歧义而不是随机取一个。
     * </p>
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_user WHERE phone = #{phone} AND deleted = 0")
    List<SysUser> selectListByPhoneForLogin(@Param("phone") String phone);

    /**
     * 根据邮箱查询用户
     */
    SysUser selectByEmail(@Param("email") String email, @Param("tenantId") Long tenantId);

    /**
     * 查询用户的角色列表
     */
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 查询用户的权限列表
     */
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    /**
     * 查询用户的菜单ID列表
     */
    List<Long> selectMenuIdsByUserId(@Param("userId") Long userId);

    /**
     * 分页查询用户列表
     */
    Page<SysUser> selectUserPage(Page<SysUser> page, 
                                   @Param("tenantId") Long tenantId,
                                   @Param("username") String username,
                                   @Param("status") Integer status,
                                   @Param("deptId") Long deptId);

    /**
     * 更新最后登录信息
     */
    int updateLoginInfo(@Param("userId") Long userId, 
                        @Param("loginIp") String loginIp);
}