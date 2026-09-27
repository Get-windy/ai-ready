package cn.aiedge.erp.printing.support;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 打印模块取当前租户。
 *
 * 单独抽出来是因为这段逻辑原先在控制器里被复制了多份，而租户取错的后果是
 * 「模板明明在库里却查不到」这种很难排查的现象。全模块只走这一个入口。
 */
public final class PrintTenantSupport {

    /** 取不到登录会话时的兜底租户（与既有业务数据同租户） */
    public static final long DEFAULT_TENANT_ID = 1L;

    private PrintTenantSupport() {
    }

    public static Long currentTenantId() {
        return currentTenantId(null);
    }

    /** 当前登录用户；无登录上下文（定时任务/内部调用）返回 null */
    public static Long currentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    /** 显式传了租户就用显式的（内部调用/定时任务场景），否则取登录会话 */
    public static Long currentTenantId(Long explicit) {
        if (explicit != null) {
            return explicit;
        }
        try {
            Object sessionTenantId = StpUtil.getSession().get("tenantId");
            if (sessionTenantId instanceof Number) {
                return ((Number) sessionTenantId).longValue();
            }
        } catch (Exception ignored) {
            // 无登录上下文（定时任务/内部调用）→ 走兜底租户
        }
        return DEFAULT_TENANT_ID;
    }
}
