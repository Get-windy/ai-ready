package cn.aiedge.base.websocket;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 通知控制器
 * <p>
 * 提供 SSE (Server-Sent Events) 端点，用于向浏览器推送实时通知。
 * 目前主要用于权限缓存失效通知，当权限变更时自动推送事件给前端，
 * 前端收到事件后自动重新加载权限数据。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@Tag(name = "SSE通知", description = "SSE 实时推送通知")
public class SseNotificationController {

    private final SseNotificationService sseService;

    /**
     * 建立 SSE 连接（需登录）
     * <p>
     * 前端通过 EventSource 连接此端点，建立连接后服务端可主动推送事件。
     * 主要事件类型：
     * - {@code connected} — 连接建立确认
     * - {@code cache-invalidate} — 缓存失效通知，前端应重新加载权限/角色
     * </p>
     */
    @GetMapping(value = "/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaCheckLogin
    @Operation(summary = "建立 SSE 通知连接", hidden = true)
    public SseEmitter subscribe() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("用户 {} 请求建立 SSE 连接", userId);
        return sseService.createEmitter(userId);
    }
}
