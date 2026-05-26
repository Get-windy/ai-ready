package cn.aiedge.base.service;

import cn.aiedge.base.entity.User;
import cn.aiedge.common.dto.user.UserCreateRequest;
import cn.aiedge.common.dto.user.UserQueryRequest;
import cn.aiedge.common.dto.user.UserUpdateRequest;
import cn.aiedge.common.dto.user.UserVO;
import cn.aiedge.common.result.PageResult;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 分页查询用户
     */
    PageResult<UserVO> pageList(UserQueryRequest request);

    /**
     * 根据ID获取用户详情
     */
    UserVO getDetail(Long id);

    /**
     * 创建用户
     */
    Long create(UserCreateRequest request);

    /**
     * 更新用户
     */
    void update(UserUpdateRequest request);

    /**
     * 删除用户
     */
    void delete(Long id);

    /**
     * 批量删除用户
     */
    void batchDelete(List<Long> ids);

    /**
     * 修改密码
     */
    void changePassword(Long id, String oldPassword, String newPassword);

    /**
     * 重置密码
     */
    void resetPassword(Long id, String newPassword);

    /**
     * 启用/禁用用户
     */
    void updateStatus(Long id, Integer status);

    /**
     * 分配角色
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 根据用户名查询用户
     */
    User getByUsername(String username);

    /**
     * 根据手机号查询用户
     */
    User getByPhone(String phone);

    /**
     * 根据邮箱查询用户
     */
    User getByEmail(String email);

    /**
     * 更新登录信息
     */
    void updateLoginInfo(Long userId, String loginIp);

    /**
     * 获取用户角色编码列表
     */
    List<String> getRoleCodes(Long userId);

    /**
     * 获取用户权限编码列表
     */
    List<String> getPermissionCodes(Long userId);
}
