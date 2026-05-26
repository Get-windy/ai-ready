package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
     * 根据用户名查询用户
     */
    SysUser selectByUsername(@Param("username") String username, @Param("tenantId") Long tenantId);

    /**
     * 根据手机号查询用户
     */
    SysUser selectByPhone(@Param("phone") String phone, @Param("tenantId") Long tenantId);

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