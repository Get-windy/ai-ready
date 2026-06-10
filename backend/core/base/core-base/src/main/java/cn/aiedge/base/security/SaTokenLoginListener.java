package cn.aiedge.base.security;

import cn.aiedge.base.entity.SysOperLog;
import cn.aiedge.base.service.SysOperLogService;
import cn.dev33.satoken.listener.SaTokenListener;
import cn.dev33.satoken.stp.SaLoginModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Sa-Token 登录事件监听器
 * <p>
 * 监听登录/踢下线事件，记录审计日志，实现并发登录监控。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SaTokenLoginListener implements SaTokenListener {

    private final SysOperLogService sysOperLogService;

    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginModel loginModel) {
        // 静默记录登录事件，不做额外处理
        log.debug("用户登录: loginType={}, loginId={}, device={}", loginType, loginId, loginModel.getDevice());
    }

    @Override
    public void doLogout(String loginType, Object loginId, String tokenValue) {
        log.debug("用户登出: loginType={}, loginId={}", loginType, loginId);
    }

    @Override
    public void doKickout(String loginType, Object loginId, String tokenValue) {
        Long userId = parseUserId(loginId);
        log.warn("用户被强制踢下线: loginType={}, loginId={}", loginType, loginId);
        recordSecurityLog(userId, "强制踢下线", "用户被管理员强制踢下线");
    }

    @Override
    public void doReplaced(String loginType, Object loginId, String tokenValue) {
        Long userId = parseUserId(loginId);
        log.warn("用户被顶替下线（多地登录）: loginType={}, loginId={}", loginType, loginId);
        recordSecurityLog(userId, "顶替下线", "因多地并发登录，旧会话被新登录顶替下线");
    }

    @Override
    public void doDisable(String loginType, Object loginId, String service, int disableTime, String realm) {
        // 暂不处理
    }

    @Override
    public void doUntieDisable(String loginType, Object loginId, String service) {
        // 暂不处理
    }

    @Override
    public void doOpenSafe(String loginType, Object loginId, String service, int safeTime) {
        // 暂不处理
    }

    @Override
    public void doCloseSafe(String loginType, Object loginId, String service) {
        // 暂不处理
    }

    @Override
    public void doCreateSession(String id) {
        // 暂不处理
    }

    @Override
    public void doLogoutSession(String id) {
        // 暂不处理
    }

    @Override
    public void doRenewTimeout(String loginType, Object loginId, String tokenValue) {
        // 暂不处理
    }

    @Override
    public void doRegisterComponent(Object compObj) {
        // 暂不处理
    }

    @Override
    public void doSetStpLogic(Object stpLogic) {
        // 暂不处理
    }

    @Override
    public void doSetConfig(Object config) {
        // 暂不处理
    }

    // ==================== 私有方法 ====================

    private Long parseUserId(Object loginId) {
        if (loginId == null) return null;
        try {
            return Long.parseLong(loginId.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void recordSecurityLog(Long userId, String action, String desc) {
        try {
            SysOperLog operLog = new SysOperLog();
            operLog.setUserId(userId);
            operLog.setModule("安全审计");
            operLog.setAction(action);
            operLog.setMethod("SaTokenLoginListener");
            operLog.setStatus(0);
            operLog.setOperTime(LocalDateTime.now());
            operLog.setErrorMsg(desc);
            sysOperLogService.recordLogAsync(operLog);
        } catch (Exception e) {
            log.warn("记录安全审计日志失败", e);
        }
    }
}
