package cn.aiedge.erp.b2b.service;

import cn.aiedge.erp.b2b.dto.LoginRequest;
import cn.aiedge.erp.b2b.dto.LoginResponse;
import cn.aiedge.erp.b2b.dto.UserInfo;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MallAuthServiceImpl implements MallAuthService {

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("用户登录: {}", request.getUsername());

        // TODO: Implement actual user authentication
        // For now, simulate a successful login
        String userId = IdUtil.fastSimpleUUID();
        StpUtil.login(userId);

        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        UserInfo userInfo = new UserInfo();
        userInfo.setId(userId);
        userInfo.setUsername(request.getUsername());
        userInfo.setNickname(request.getUsername());
        userInfo.setLevel("普通会员");
        userInfo.setPoints(0);
        userInfo.setBalance(0.0);

        LoginResponse response = new LoginResponse();
        response.setToken(tokenInfo.getTokenValue());
        response.setUser(userInfo);

        log.info("用户登录成功: {}, token: {}", request.getUsername(), tokenInfo.getTokenValue());
        return response;
    }

    @Override
    @Transactional
    public void register(LoginRequest request) {
        log.info("用户注册: {}", request.getUsername());

        // TODO: Implement actual user registration
        log.info("用户注册成功: {}", request.getUsername());
    }

    @Override
    public void logout() {
        log.info("用户登出");
        StpUtil.logout();
    }

    @Override
    public String refreshToken() {
        log.info("刷新token");
        // TODO: Implement token refresh logic
        StpUtil.login(StpUtil.getLoginIdAsString());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return tokenInfo.getTokenValue();
    }
}
