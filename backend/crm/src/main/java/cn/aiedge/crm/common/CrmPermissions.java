package cn.aiedge.crm.common;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;

import java.util.List;

/**
 * CRM 模块的权限校验支持。
 *
 * <p>本模块的控制器位于 {@code crm} 子模块，只依赖 {@code core-base}，拿不到
 * {@code core-api} 里的 {@code @RequirePermission} 注解与切面；因此改用 Sa-Token 的
 * 静态 API 做显式校验，语义与 {@code PermissionAspect} 保持一致。
 *
 * <p><b>超管口径</b>：{@code UnifiedPermissionCacheService} 对超管角色返回单元素列表
 * {@code ["*"]}，此处显式放行 —— 不要依赖 Sa-Token 的通配符策略（其行为随版本而异）。
 *
 * <p>校验失败抛 {@link NotPermissionException}，由 {@code GlobalExceptionHandler} 统一
 * 转换为 HTTP 403 「无权限访问: xxx」。
 */
public final class CrmPermissions {

    /** 超管通配权限码 */
    private static final String WILDCARD = "*";

    private CrmPermissions() {
    }

    /**
     * 要求当前登录用户具备指定权限码。超管（{@code *}）直接放行。
     *
     * @param code 权限码，如 {@code crm:contract:approve}
     */
    public static void require(String code) {
        List<String> permissions = StpUtil.getPermissionList();
        if (permissions != null && (permissions.contains(WILDCARD) || permissions.contains(code))) {
            return;
        }
        throw new NotPermissionException(code);
    }
}
