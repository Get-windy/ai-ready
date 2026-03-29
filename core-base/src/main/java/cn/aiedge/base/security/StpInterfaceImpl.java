package cn.aiedge.base.security;

import cn.aiedge.base.service.SysUserService;
import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限验证实现
 * 实现 StpInterface 接口，提供权限和角色查询
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SysUserService userService;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<String> permissions = new ArrayList<>();
        try {
            Long userId = Long.parseLong(loginId.toString());
            permissions = userService.getUserPermissionCodes(userId);
            log.debug("用户 {} 的权限列表: {}", userId, permissions);
        } catch (Exception e) {
            log.error("获取用户权限失败: {}", e.getMessage());
        }
        return permissions;
    }

    /**
     * 返回一个账号所拥有的角色标识集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> roles = new ArrayList<>();
        try {
            Long userId = Long.parseLong(loginId.toString());
            roles = userService.getUserRoleCodes(userId);
            log.debug("用户 {} 的角色列表: {}", userId, roles);
        } catch (Exception e) {
            log.error("获取用户角色失败: {}", e.getMessage());
        }
        return roles;
    }
}