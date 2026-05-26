package cn.aiedge.base.service;

import cn.aiedge.common.dto.auth.LoginRequest;
import cn.aiedge.common.dto.auth.LoginVO;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     */
    LoginVO login(LoginRequest request, String clientIp);

    /**
     * 用户登出
     */
    void logout();

    /**
     * 刷新令牌
     */
    LoginVO refreshToken(String refreshToken);

    /**
     * 获取当前登录用户信息
     */
    LoginVO.UserInfo getCurrentUserInfo();

    /**
     * 检查用户名是否可用
     */
    boolean checkUsernameAvailable(String username);
}
