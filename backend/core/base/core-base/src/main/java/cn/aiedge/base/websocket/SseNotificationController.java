package cn.aiedge.base.websocket;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.exception.NotLoginException;

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
     * 前端通过 EventSource 连接此端点（token 通过 URL 查询参数传递，
     * 因为 EventSource API 不支持自定义请求头），建立连接后服务端可主动推送事件。
     * 主要事件类型：
     * - {@code connected} — 连接建立确认
     * - {@code cache-invalidate} — 缓存失效通知，前端应重新加载权限/角色
     * </p>
     */
    @GetMapping(value = "/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "建立 SSE 通知连接", hidden = true)
    public SseEmitter subscribe(@RequestParam("token") String token, HttpServletRequest request) {
        try {
            // 验证token是否为空
            if (token == null || token.trim().isEmpty()) {
                log.debug("SSE连接请求缺少token参数");
                return createEmptyEmitter();
            }

            // EventSource API 不支持自定义请求头，token 通过 URL 参数传递
            // 使用Sa-Token的getLoginIdByToken直接验证token，不依赖会话状态
            Object loginId = StpUtil.getLoginIdByToken(token.trim());
            if (loginId != null) {
                Long userId = Long.valueOf(loginId.toString());
                log.info("用户 {} SSE连接已建立", userId);
                // 发送连接确认事件
                SseEmitter emitter = sseService.createEmitter(userId);
                try {
                    emitter.send(SseEmitter.event()
                        .name("connected")
                        .data("{\"userId\":" + userId + ",\"message\":\"SSE连接已建立\"}"));
                } catch (Exception sendEx) {
                    log.debug("发送SSE确认事件失败: {}", sendEx.getMessage());
                }
                return emitter;
            } else {
                log.debug("SSE token无效: loginId为null");
                return createEmptyEmitter();
            }
        } catch (NotLoginException e) {
            // token无效或已过期，静默处理，返回空流避免前端控制台报错
            log.debug("SSE token验证失败: {}", e.getMessage());
            return createEmptyEmitter();
        } catch (Exception e) {
            // 其他异常也静默处理
            log.warn("SSE连接异常: {}", e.getMessage());
            return createEmptyEmitter();
        }
    }

    /**
     * 创建空的SSE Emitter，立即完成以避免资源占用
     */
    private SseEmitter createEmptyEmitter() {
        SseEmitter emitter = new SseEmitter(0L);
        try {
            emitter.complete();
        } catch (Exception e) {
            // ignore
        }
        return emitter;
    }
}
