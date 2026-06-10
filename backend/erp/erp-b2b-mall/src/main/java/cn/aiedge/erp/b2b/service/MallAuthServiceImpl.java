package cn.aiedge.erp.b2b.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.b2b.dto.LoginRequest;
import cn.aiedge.erp.b2b.dto.LoginResponse;
import cn.aiedge.erp.b2b.dto.UserInfo;
import cn.aiedge.erp.b2b.mapper.ShopUserMapper;
import cn.aiedge.erp.b2b.model.ShopUser;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
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

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("用户登录: {}", request.getUsername());

        // 查询数据库中的用户
        ShopUser user = shopUserMapper.selectOne(
            new LambdaQueryWrapper<ShopUser>()
                .eq(ShopUser::getUsername, request.getUsername())
                .eq(ShopUser::getDeleted, 0)
                .last("LIMIT 1")
        );
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw BusinessException.badRequest("账户已被禁用");
        }

        // 验证密码
        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw BusinessException.badRequest("密码错误");
        }

        // SaToken登录
        StpUtil.login(user.getId());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        shopUserMapper.updateById(user);

        // 构建响应
        UserInfo userInfo = new UserInfo();
        userInfo.setId(String.valueOf(user.getId()));
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        if (user.getCompanyName() != null && !user.getCompanyName().isEmpty()) {
            userInfo.setLevel("企业会员");
        } else {
            userInfo.setLevel("普通会员");
        }
        userInfo.setPoints(0);
        userInfo.setBalance(0.0);

        LoginResponse response = new LoginResponse();
        response.setToken(tokenInfo.getTokenValue());
        response.setUser(userInfo);

        log.info("用户登录成功: {}, userId={}", request.getUsername(), user.getId());
        return response;
    }

    @Override
    @Transactional
    public void register(LoginRequest request) {
        log.info("用户注册: {}", request.getUsername());

        // 检查用户名是否已存在
        ShopUser existing = shopUserMapper.selectOne(
            new LambdaQueryWrapper<ShopUser>()
                .eq(ShopUser::getUsername, request.getUsername())
                .eq(ShopUser::getDeleted, 0)
                .last("LIMIT 1")
        );
        if (existing != null) {
            throw BusinessException.badRequest("用户名已存在");
        }

        // 创建新用户
        ShopUser user = new ShopUser();
        user.setUsername(request.getUsername());
        user.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
        user.setNickname(request.getUsername());
        user.setSource("h5");
        user.setAuditStatus(1); // 默认通过
        user.setStatus(1); // 正常
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        shopUserMapper.insert(user);

        log.info("用户注册成功: {}, userId={}", request.getUsername(), user.getId());
    }

    @Override
    public void logout() {
        log.info("用户登出");
        StpUtil.logout();
    }

    @Override
    public String refreshToken() {
        log.info("刷新token, 当前用户: {}", StpUtil.getLoginIdDefaultNull());

        // 验证当前用户是否已登录
        if (StpUtil.isLogin()) {
            // 续期当前会话并获取新token
            StpUtil.renewTimeout(StpUtil.getTokenTimeout());
            SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
            log.info("token刷新成功: {}", tokenInfo.getTokenValue());
            return tokenInfo.getTokenValue();
        }

        throw BusinessException.unauthorized("用户未登录，无法刷新token");
    }
}
