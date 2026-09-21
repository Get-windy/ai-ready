package cn.aiedge.permission.service;

import cn.aiedge.base.security.PermissionSimulationHolder;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 权限模拟服务
 * <p>
 * 允许管理员以其他用户的权限上下文进行"what-if"验证：模拟期间 {@code @SaCheckPermission}
 * 之类的判定按**被模拟用户**计算（落地点在 core-base 的 {@code StpInterfaceImpl#resolveEffectiveUserId}）。
 * 不影响被模拟用户的真实权限，也不改变模拟者自身的会话身份。
 * </p>
 *
 * <p>两种用法：</p>
 * <ul>
 *   <li><b>持久模拟</b>：{@code POST /api/simulate/start} 写入 Sa-Token Session，
 *       之后所有请求都生效，直到 {@code POST /api/simulate/stop}。</li>
 *   <li><b>单次模拟</b>：请求头 {@code X-Simulate-User-Id}，只影响该次请求。</li>
 * </ul>
 *
 * <p>⚠️ 本类此前把模拟态写进自己的 ThreadLocal，而 ThreadLocal 在请求结束就被过滤器清理，
 * 且没有任何权限判定逻辑读取它 —— 即整个功能是空壳（2026-09-20 修复：改为 Session 持久化 +
 * 由 StpInterfaceImpl 消费）。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionSimulationService {

    /** 请求头名称：模拟用户ID */
    public static final String SIMULATE_USER_HEADER = "X-Simulate-User-Id";

    /** 请求头名称：模拟原因 */
    public static final String SIMULATE_REASON_HEADER = "X-Simulate-Reason";

    /**
     * 开始权限模拟（写入 Sa-Token Session，跨请求持续生效）
     *
     * @param targetUserId 目标用户ID
     * @param reason       模拟原因（审计用）
     * @throws RuntimeException 如果当前用户不是管理员
     */
    public void startSimulation(Long targetUserId, String reason) {
        if (!StpUtil.isLogin()) {
            throw new RuntimeException("未登录，无法使用权限模拟");
        }

        if (!StpUtil.hasPermission("system:simulate")) {
            throw new RuntimeException("无权限模拟权限，需要 system:simulate 权限");
        }

        if (targetUserId == null) {
            throw new RuntimeException("模拟用户ID不能为空");
        }

        // 不能模拟自己
        long currentUserId = StpUtil.getLoginIdAsLong();
        if (currentUserId == targetUserId) {
            throw new RuntimeException("不能模拟自己的权限");
        }

        String finalReason = (reason != null && !reason.isBlank()) ? reason : "未填写模拟原因";
        // 写入 Session —— 这是让模拟跨请求持续生效的关键（ThreadLocal 活不过一个请求）
        StpUtil.getSession().set(PermissionSimulationHolder.SESSION_KEY, targetUserId);
        StpUtil.getSession().set(PermissionSimulationHolder.SESSION_REASON_KEY, finalReason);

        log.info("权限模拟开始: adminUserId={}, targetUserId={}, reason={}",
                currentUserId, targetUserId, finalReason);
    }

    /**
     * 结束权限模拟
     */
    public void stopSimulation() {
        try {
            Object target = StpUtil.getSession().get(PermissionSimulationHolder.SESSION_KEY);
            if (target != null) {
                log.info("权限模拟结束: adminUserId={}, targetUserId={}",
                        getActualUserId(), target);
            }
            StpUtil.getSession().delete(PermissionSimulationHolder.SESSION_KEY);
            StpUtil.getSession().delete(PermissionSimulationHolder.SESSION_REASON_KEY);
        } catch (Exception e) {
            // 无会话上下文时只清理请求级模拟，不抛错（stop 应当是幂等的）
            log.warn("结束权限模拟时清理会话失败: {}", e.getMessage());
        } finally {
            PermissionSimulationHolder.clearCurrentRequest();
        }
    }

    /**
     * 当前是否正在模拟其他用户的权限
     */
    public boolean isSimulating() {
        return getSimulatedUserId() != null;
    }

    /**
     * 获取被模拟的用户ID；未模拟时为 null
     */
    public Long getSimulatedUserId() {
        // 单次模拟（当前请求）优先
        Long singleRequest = PermissionSimulationHolder.getCurrentRequestTarget();
        if (singleRequest != null) {
            return singleRequest;
        }
        try {
            Object v = StpUtil.getSession().get(PermissionSimulationHolder.SESSION_KEY);
            return v == null ? null : Long.parseLong(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取模拟原因
     */
    public String getSimulateReason() {
        String singleRequest = PermissionSimulationHolder.getCurrentRequestReason();
        if (singleRequest != null) {
            return singleRequest;
        }
        try {
            Object v = StpUtil.getSession().get(PermissionSimulationHolder.SESSION_REASON_KEY);
            return v == null ? null : v.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前实际用户ID（不受模拟影响）
     */
    public long getActualUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 检查当前请求是否包含模拟头信息，如果有则自动应用（只影响本次请求）
     *
     * @param simulateUserId  请求头中的模拟用户ID
     * @param simulateReason  请求头中的模拟用户理由
     */
    public void applyFromRequest(String simulateUserId, String simulateReason) {
        if (simulateUserId != null && !simulateUserId.isEmpty()) {
            try {
                Long targetId = Long.parseLong(simulateUserId);
                PermissionSimulationHolder.setCurrentRequestTarget(targetId);
                PermissionSimulationHolder.setCurrentRequestReason(
                        (simulateReason != null && !simulateReason.isBlank()) ? simulateReason : "请求头单次模拟");
                log.info("请求头单次权限模拟: adminUserId={}, targetUserId={}", getActualUserId(), targetId);
            } catch (NumberFormatException e) {
                log.warn("模拟用户ID格式错误: {}", simulateUserId);
            }
        }
    }

    /**
     * 清理当前请求级模拟态（请求结束时调用）
     */
    public void cleanup() {
        PermissionSimulationHolder.clearCurrentRequest();
    }
}
