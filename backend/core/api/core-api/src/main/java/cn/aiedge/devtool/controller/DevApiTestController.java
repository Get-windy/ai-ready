package cn.aiedge.devtool.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.devtool.config.ApiTestProperties;
import cn.aiedge.devtool.dto.ApiTestSendRequest;
import cn.aiedge.devtool.dto.ApiTestSendResult;
import cn.aiedge.devtool.service.ApiTestOutboundService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * API 测试台（系统 → 开发工具 → API测试，菜单 62404）后端端点。
 *
 * <p>本端点是全平台**唯一一个「用户输入什么、服务端就往哪发请求」**的接口，
 * 因此它的功能面只有一条、而安全面有十条。十条闸门的落点见
 * {@link cn.aiedge.devtool.service.ApiTestOutboundService} 与
 * {@link cn.aiedge.devtool.security.ApiTestTargetGuard} 的类注释。
 *
 * <p><b>权限</b>：{@code system:dev:api-test:send}（种子见 Flyway V11.418.0）。
 * 页面可见性由菜单 {@code client_type=system-admin} 决定，但那只是「看得见」；
 * 动作层必须另有权限码，否则任何登录用户直接打本端点即可把服务端当跳板。
 *
 * <p><b>审计</b>：{@code @OperationLog} 落 {@code sys_oper_log}，记录
 * 调用者（userId/username）、时间、入站 URI、执行耗时与是否异常；
 * **目标 URL 与请求方法**随请求参数一并落库（请求体与请求头已在 DTO 上标 WRITE_ONLY，
 * 不写审计表 —— 审计要「谁打了哪个地址」，不需要把业务数据与凭据抄一份）。
 *
 * <p><b>本系统安全设计（非业界标准）</b>：OWASP《SSRF Prevention Cheat Sheet》
 * 没有针对「API 测试/调试工具」的章节；「调试台剥离平台凭据 + 响应大小/超时上限」
 * 是本系统自建口径（D 级），**不得写成业界标准**。
 *
 * @since 2026-09-19
 */
@Slf4j
@RestController
@RequestMapping("/api/dev/api-test")
@RequiredArgsConstructor
@Tag(name = "开发工具-API测试", description = "受控出站请求（本系统 API 调试台，服务端 allowlist + 凭据剥离 + 调用审计）")
public class DevApiTestController {

    /** 动作权限码（V11.418.0 落库） */
    public static final String PERMISSION_SEND = "system:dev:api-test:send";

    private final ApiTestProperties properties;
    private final ApiTestOutboundService outboundService;
    private final RateLimiter rateLimiter = new RateLimiter();

    /**
     * 查询当前**服务端实际生效**的安全边界。
     * <p>给页面用：页面上的「安全边界说明」不该是前端硬编码的文案，而应来自服务端事实
     * （self-base-url 是什么、是否放行外部、凭据能不能自带、响应上限多少）。
     */
    @GetMapping("/policy")
    @SaCheckPermission(PERMISSION_SEND)
    @Operation(summary = "查询 API 测试台的服务端安全边界")
    public ResponseEntity<Map<String, Object>> policy() {
        return ResponseEntity.ok(outboundService.currentPolicy());
    }

    /**
     * 发送一次受控出站请求。
     *
     * <p>返回体是 {@link ApiTestSendResult} 的扁平结构：{@code success=false} 表示
     * **请求根本没发出去**（被闸门拒绝 / 参数非法 / 超时 / 连接失败），{@code rejectReason}
     * 是可直接展示的中文原因；{@code success=true} 表示已拿到目标服务端的真实响应
     * （响应是 4xx/5xx 时仍是 success=true —— 那是目标的结论，不是本端点的失败）。
     */
    @PostMapping("/send")
    @SaCheckPermission(PERMISSION_SEND)
    @OperationLog(module = "开发工具-API测试", type = "OTHER", desc = "受控出站请求（API 测试台）")
    @Operation(summary = "发送受控出站请求（服务端 allowlist + 凭据剥离）")
    public ResponseEntity<ApiTestSendResult> send(@RequestBody ApiTestSendRequest request) {
        String requestedUrl = request == null ? null : request.getUrl();
        Long userId = currentUserId();

        if (!properties.isEnabled()) {
            log.warn("[API测试台] 端点已被服务端关闭，拒绝调用: userId={} url={}", userId, requestedUrl);
            return ResponseEntity.ok(ApiTestSendResult.rejected(
                    "API 测试台已被服务端关闭（app.api-test.enabled=false）：本页在生产环境应关闭或下线", requestedUrl));
        }

        // 闸门 10：限流（防被当成扫描器 / 内网探测工具）
        if (!rateLimiter.tryAcquire(userId, properties.getRateLimitPerMinute())) {
            log.warn("[API测试台] 触发限流: userId={} limit={}/min url={}",
                    userId, properties.getRateLimitPerMinute(), requestedUrl);
            return ResponseEntity.ok(ApiTestSendResult.rejected(
                    "调用过于频繁：本端点每分钟最多 " + properties.getRateLimitPerMinute()
                            + " 次（限流是防「把调试台当扫描器用」的兜底）", requestedUrl));
        }

        ApiTestSendResult result = outboundService.send(request);
        if (!result.success()) {
            // 被拒绝也留痕：审计要回答的是「谁在什么时候试图打哪个地址」
            log.warn("[API测试台] 请求未发出: userId={} url={} reason={}", userId, requestedUrl, result.rejectReason());
        }
        return ResponseEntity.ok(result);
    }

    private Long currentUserId() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 单实例内存滑动窗口限流。
     *
     * <p><b>已知边界</b>：这是进程内计数，多实例部署时每个实例各算一份（不是分布式限流），
     * 且重启即清零。作为「防扫描器」的兜底足够；若要严格口径需换成 Redis 计数。
     * 内存占用有上限：每个 key 最多 retain {@code limit} 个时间戳，超过 {@code MAX_KEYS} 时整体清理一次。
     */
    private static final class RateLimiter {

        private static final long WINDOW_MS = 60_000L;
        private static final int MAX_KEYS = 10_000;
        private static final int CLEANUP_EVERY = 256;

        private final Map<Long, Deque<Long>> hits = new ConcurrentHashMap<>();
        private final AtomicInteger counter = new AtomicInteger();

        private boolean tryAcquire(Long key, int limitPerMinute) {
            if (limitPerMinute <= 0) {
                return true;
            }
            Long safeKey = key == null ? -1L : key;
            if (counter.incrementAndGet() % CLEANUP_EVERY == 0 || hits.size() > MAX_KEYS) {
                cleanup();
            }
            long now = System.currentTimeMillis();
            long windowStart = now - WINDOW_MS;
            Deque<Long> stamps = hits.computeIfAbsent(safeKey, k -> new ArrayDeque<>());
            synchronized (stamps) {
                while (!stamps.isEmpty() && stamps.peekFirst() < windowStart) {
                    stamps.pollFirst();
                }
                if (stamps.size() >= limitPerMinute) {
                    return false;
                }
                stamps.addLast(now);
                return true;
            }
        }

        private void cleanup() {
            long windowStart = System.currentTimeMillis() - WINDOW_MS;
            hits.entrySet().removeIf(entry -> {
                Deque<Long> stamps = entry.getValue();
                synchronized (stamps) {
                    while (!stamps.isEmpty() && stamps.peekFirst() < windowStart) {
                        stamps.pollFirst();
                    }
                    return stamps.isEmpty();
                }
            });
        }
    }
}
