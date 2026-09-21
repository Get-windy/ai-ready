package cn.aiedge.base.security;

/**
 * 权限模拟上下文（跨模块共享的桥）
 *
 * <p>「管理员以他人身份查看权限」需要两处协作：core-api 的 {@code /api/simulate/*} 负责写入，
 * core-base 的 {@link StpInterfaceImpl} 负责读取。两个模块不存在依赖关系，故用本类承载共享状态。</p>
 *
 * <p>支持两种生效范围：</p>
 * <ul>
 *   <li><b>持久模拟</b> —— 写入 Sa-Token Session（{@link #SESSION_KEY}），后续每个请求都生效，
 *       直到显式调用 {@code /api/simulate/stop}。</li>
 *   <li><b>单次模拟</b> —— 请求头 {@code X-Simulate-User-Id}，由 core-api 的
 *       {@code PermissionSimulationFilter} 写入本 ThreadLocal，只影响当前这一次请求。</li>
 * </ul>
 *
 * <p>⚠️ 此前模拟态**只存在 ThreadLocal 里**，而 ThreadLocal 又由过滤器在请求结束时清理，
 * 且没有任何权限判定逻辑读取它 —— 即整个权限模拟是个空壳（2026-09-20 盘点确认）。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public final class PermissionSimulationHolder {

    /** Sa-Token Session 键：被模拟用户 ID（持久模拟） */
    public static final String SESSION_KEY = "simulateUserId";

    /** Sa-Token Session 键：模拟原因（写入审计日志） */
    public static final String SESSION_REASON_KEY = "simulateReason";

    /** 单次模拟（当前请求）的目标用户 ID */
    private static final ThreadLocal<Long> CURRENT_REQUEST_TARGET = new ThreadLocal<>();

    /** 单次模拟（当前请求）的原因，用于审计 */
    private static final ThreadLocal<String> CURRENT_REQUEST_REASON = new ThreadLocal<>();

    private PermissionSimulationHolder() {
    }

    /** 设置当前请求的模拟目标（由过滤器在解析请求头后调用） */
    public static void setCurrentRequestTarget(Long userId) {
        if (userId == null) {
            CURRENT_REQUEST_TARGET.remove();
        } else {
            CURRENT_REQUEST_TARGET.set(userId);
        }
    }

    /** 获取当前请求的模拟目标；未模拟时为 null */
    public static Long getCurrentRequestTarget() {
        return CURRENT_REQUEST_TARGET.get();
    }

    /** 设置当前请求的模拟原因 */
    public static void setCurrentRequestReason(String reason) {
        if (reason == null) {
            CURRENT_REQUEST_REASON.remove();
        } else {
            CURRENT_REQUEST_REASON.set(reason);
        }
    }

    /** 获取当前请求的模拟原因；未模拟时为 null */
    public static String getCurrentRequestReason() {
        return CURRENT_REQUEST_REASON.get();
    }

    /** 清理当前请求的模拟态（过滤器 finally 中调用，防止线程复用导致的串味） */
    public static void clearCurrentRequest() {
        CURRENT_REQUEST_TARGET.remove();
        CURRENT_REQUEST_REASON.remove();
    }
}
