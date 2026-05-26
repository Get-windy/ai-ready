package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.User;
import cn.aiedge.base.service.AuthService;
import cn.aiedge.base.service.UserService;
import cn.aiedge.common.dto.auth.LoginRequest;
import cn.aiedge.common.dto.auth.LoginVO;
import cn.aiedge.common.exception.BusinessException;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaTokenConsts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 认证服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginVO login(LoginRequest request, String clientIp) {
        // 查询用户
        User user = userService.getByUsername(request.getUsername());
        if (user == null) {
            throw BusinessException.badRequest("用户名或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() != 1) {
            throw BusinessException.badRequest("用户已被禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw BusinessException.badRequest("用户名或密码错误");
        }

        // Sa-Token 登录
        StpUtil.login(user.getId());

        // 获取Token信息
        String tokenValue = StpUtil.getTokenValue();
        long tokenTimeout = StpUtil.getTokenTimeout();

        // 更新登录信息
        userService.updateLoginInfo(user.getId(), clientIp);

        // 构建响应
        LoginVO vo = new LoginVO();
        vo.setAccessToken(tokenValue);
        vo.setExpiresIn(tokenTimeout);

        // 用户信息
        LoginVO.UserInfo userInfo = new LoginVO.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setDeptId(user.getDeptId());

        // 角色和权限
        List<String> roles = userService.getRoleCodes(user.getId());
        List<String> permissions = userService.getPermissionCodes(user.getId());
        userInfo.setRoles(roles);
        userInfo.setPermissions(permissions);

        vo.setUserInfo(userInfo);

        log.info("用户登录成功: {}", user.getUsername());
        return vo;
    }

    @Override
    public void logout() {
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            StpUtil.logout();
            log.info("用户登出成功: userId={}", userId);
        }
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        // TODO: 实现刷新令牌逻辑
        throw BusinessException.badRequest("暂不支持刷新令牌");
    }

    @Override
    public LoginVO.UserInfo getCurrentUserInfo() {
        if (!StpUtil.isLogin()) {
            throw BusinessException.unauthorized("用户未登录");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        LoginVO.UserInfo userInfo = new LoginVO.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setDeptId(user.getDeptId());

        List<String> roles = userService.getRoleCodes(userId);
        List<String> permissions = userService.getPermissionCodes(userId);
        userInfo.setRoles(roles);
        userInfo.setPermissions(permissions);

        return userInfo;
    }

    @Override
    public boolean checkUsernameAvailable(String username) {
        return userService.getByUsername(username) == null;
    }
}
