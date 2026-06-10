package cn.aiedge.erp.b2b.service.impl;

import cn.aiedge.base.security.PasswordEncryptor;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.b2b.dto.LoginRequest;
import cn.aiedge.erp.b2b.dto.LoginResponse;
import cn.aiedge.erp.b2b.dto.UserInfo;
import cn.aiedge.erp.b2b.mapper.ShopConfigMapper;
import cn.aiedge.erp.b2b.mapper.ShopUserMapper;
import cn.aiedge.erp.b2b.model.ShopConfig;
import cn.aiedge.erp.b2b.model.ShopUser;
import cn.aiedge.erp.b2b.service.MallAuthService;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MallAuthServiceImpl implements MallAuthService {

    private final ShopUserMapper shopUserMapper;
    private final ShopConfigMapper shopConfigMapper;
    private final PasswordEncryptor passwordEncryptor;

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("商城用户登录: {}", request.getUsername());

        ShopUser user = shopUserMapper.selectOne(
                new LambdaQueryWrapper<ShopUser>()
                        .eq(ShopUser::getUsername, request.getUsername())
                        .last("LIMIT 1")
        );

        if (user == null) {
            throw BusinessException.badRequest("用户名或密码错误");
        }

        if (user.getStatus() != 1) {
            throw BusinessException.badRequest("账号已被禁用");
        }

        if (!passwordEncryptor.matches(request.getPassword(), user.getPassword())) {
            throw BusinessException.badRequest("用户名或密码错误");
        }

        // 登录到 Sa-Token
        StpUtil.login(user.getId());

        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        UserInfo userInfo = buildUserInfo(user);

        LoginResponse response = new LoginResponse();
        response.setToken(tokenInfo.getTokenValue());
        response.setUser(userInfo);

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        shopUserMapper.updateById(user);

        log.info("商城用户登录成功: {}, token: {}", request.getUsername(), tokenInfo.getTokenValue());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(LoginRequest request) {
        log.info("商城用户注册: {}", request.getUsername());

        // 检查用户名是否已存在
        Long count = shopUserMapper.selectCount(
                new LambdaQueryWrapper<ShopUser>()
                        .eq(ShopUser::getUsername, request.getUsername())
        );
        if (count != null && count > 0) {
            throw BusinessException.badRequest("用户名已存在");
        }

        // 判断是否需要审核（取第一个租户的配置，简化处理）
        boolean needAudit = true;
        ShopConfig config = shopConfigMapper.selectOne(
                new LambdaQueryWrapper<ShopConfig>()
                        .last("LIMIT 1")
        );
        if (config != null && config.getEnableAutoAudit() == 1) {
            needAudit = false;
        }

        ShopUser user = new ShopUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncryptor.encode(request.getPassword()));
        user.setNickname(request.getUsername());
        user.setSource("h5");
        user.setAuditStatus(needAudit ? 0 : 1);
        user.setAuditTime(needAudit ? null : LocalDateTime.now());
        user.setStatus(1);

        shopUserMapper.insert(user);

        log.info("商城用户注册成功: {}, 需审核: {}", request.getUsername(), needAudit);
    }

    @Override
    public void logout() {
        log.info("商城用户登出");
        StpUtil.logout();
    }

    @Override
    public String refreshToken() {
        log.info("刷新商城用户token");
        StpUtil.login(StpUtil.getLoginIdAsString());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return tokenInfo.getTokenValue();
    }

    private UserInfo buildUserInfo(ShopUser user) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(String.valueOf(user.getId()));
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setPhone(user.getPhone());
        return userInfo;
    }
}
