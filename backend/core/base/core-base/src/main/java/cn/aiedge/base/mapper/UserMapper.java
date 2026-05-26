package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     */
    User selectByUsername(@Param("username") String username);

    /**
     * 根据手机号查询用户
     */
    User selectByPhone(@Param("phone") String phone);

    /**
     * 根据邮箱查询用户
     */
    User selectByEmail(@Param("email") String email);

    /**
     * 根据角色ID查询用户列表
     */
    List<User> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据部门ID查询用户列表
     */
    List<User> selectByDeptId(@Param("deptId") Long deptId);

    /**
     * 更新用户最后登录信息
     */
    int updateLoginInfo(@Param("id") Long id, @Param("loginIp") String loginIp);
}
