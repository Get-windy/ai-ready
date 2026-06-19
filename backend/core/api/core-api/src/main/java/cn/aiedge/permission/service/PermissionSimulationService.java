package cn.aiedge.permission.service;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 权限模拟服务
 * <p>
 * 允许管理员模拟其他用户的权限上下文进行"what-if"验证。
 * 通过 ThreadLocal 实现，仅影响当前请求的权限判断，不影响被模拟用户的真实权限。
 * </p>
 *
 * 使用方式：
 * <ul>
 *   <li>API 调用时在请求头添加 {@code X-Simulate-User-Id} 即可切换模拟目标</li>
 *   <li>模拟期间操作记录在审计日志中，标注为模拟操作</li>
 * </ul>
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

    private final ThreadLocal<Long> simulatedUserId = new ThreadLocal<>();
    private final ThreadLocal<String> simulatedReason = new ThreadLocal<>();

    /**
     * 开始模拟指定用户的权限
     *
     * @param targetUserId 目标用户ID
     * @param reason       模拟原因（审计用）
     * @throws RuntimeException 如果当前用户不是管理员
     */
    public void startSimulation(Long targetUserId, String reason) {
        // 验证当前用户具有模拟权限
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

        simulatedUserId.set(targetUserId);
        simulatedReason.set(reason != null ? reason : "未填写模拟原因");

        log.info("权限模拟开始: adminUserId={}, targetUserId={}, reason={}",
                currentUserId, targetUserId, reason);
    }

    /**
     * 结束权限模拟
     */
    public void stopSimulation() {
        Long targetUserId = simulatedUserId.get();
        if (targetUserId != null) {
            log.info("权限模拟结束: targetUserId={}", targetUserId);
        }
        simulatedUserId.remove();
        simulatedReason.remove();
    }

    /**
     * 当前是否正在模拟其他用户的权限
     */
    public boolean isSimulating() {
        return simulatedUserId.get() != null;
    }

    /**
     * 获取被模拟的用户ID
     */
    public Long getSimulatedUserId() {
        return simulatedUserId.get();
    }

    /**
     * 获取模拟原因
     */
    public String getSimulateReason() {
        return simulatedReason.get();
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
     * 检查当前请求是否包含模拟头信息，如果有则自动应用
     *
     * @param simulateUserId  请求头中的模拟用户ID
     * @param simulateReason  请求头中的模拟原因
     */
    public void applyFromRequest(String simulateUserId, String simulateReason) {
        if (simulateUserId != null && !simulateUserId.isEmpty()) {
            try {
                Long targetId = Long.parseLong(simulateUserId);
                startSimulation(targetId, simulateReason);
            } catch (NumberFormatException e) {
                log.warn("模拟用户ID格式错误: {}", simulateUserId);
            }
        }
    }

    /**
     * 清理 ThreadLocal（请求结束时调用）
     */
    public void cleanup() {
        simulatedUserId.remove();
        simulatedReason.remove();
    }
}
