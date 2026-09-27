package cn.aiedge.trade.open;

import cn.aiedge.trade.mapper.ExternalChannelConfigMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

/**
 * 开放 API（`/api/open/**`）验签拦截器。
 *
 * <p><b>为什么需要它</b>（2026-09-27，配送/交易两模块审计的同一处代码）：</p>
 * <ul>
 *   <li>该组端点原先**无任何鉴权注解**，且不在 Sa-Token 白名单 ⇒
 *       <b>外部平台调不通（必然 401），而任何已登录用户反而能无权限码调用</b>
 *       （含写订单、查任意 SKU 库存）；</li>
 *   <li>类注释自称「安全验证 - 签名校验」，但 `signature` 参数收下后**从未使用**。</li>
 * </ul>
 *
 * <p><b>签名口径</b>（对外文档化，接入方按此实现）：</p>
 * <pre>
 *   sign = HEX( HMAC-SHA256( appSecret, appId + timestamp + requestURI ) )
 *   请求头：X-Api-Key / X-Timestamp / X-Sign
 *   （appId/appSecret 取自《交易 → 外部平台 → 渠道配置》的 external_channel_config）
 * </pre>
 *
 * <p><b>不拦的例外</b>：`/api/open/health` —— 健康检查无业务语义、无需凭据，
 * 放行以免接入方探活还得先签名。</p>
 *
 * <p><b>密钥未配置时的行为</b>：一律拒绝（401），且返回原因可读。
 * 这不是"功能坏了"—— 当前 `external_channel_config` 无数据，即尚无外部平台接入；
 * 拒绝是**正确**的（此前"任何登录用户可调"才是缺陷）。</p>
 *
 * <p><b>防重放</b>：时间戳容差（默认 300 秒，可配 `openapi.auth.tolerance-seconds`）。
 * 未做 nonce 台账（渠道回调用的是 `dms_channel_callback_log` 唯一索引那套）；
 * 对外开放 API 若后续接入真实平台，建议按同法补 nonce 存储。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenApiAuthInterceptor implements HandlerInterceptor {

    private final ExternalChannelConfigMapper channelConfigMapper;

    /** 时间戳容差（秒）。0 = 不校验时间戳。 */
    @Value("${openapi.auth.tolerance-seconds:300}")
    private long toleranceSeconds;

    private static final String HEADER_API_KEY = "X-Api-Key";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_SIGN = "X-Sign";

    /** 无需签名的路径（健康检查）。 */
    private static final String HEALTH_PATH = "/api/open/health";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if (path.endsWith(HEALTH_PATH)) {
            return true;
        }

        String appId = request.getHeader(HEADER_API_KEY);
        String timestamp = request.getHeader(HEADER_TIMESTAMP);
        String sign = request.getHeader(HEADER_SIGN);

        if (isBlank(appId) || isBlank(timestamp) || isBlank(sign)) {
            return reject(response, "缺少鉴权头（需 " + HEADER_API_KEY + " / " + HEADER_TIMESTAMP + " / " + HEADER_SIGN + "）");
        }

        // ① 时间戳容差
        long ts;
        try {
            ts = Long.parseLong(timestamp.trim());
        } catch (NumberFormatException e) {
            return reject(response, "时间戳格式非法: " + timestamp);
        }
        long millis = ts < 1_000_000_000_000L ? ts * 1000 : ts;
        if (toleranceSeconds > 0) {
            long drift = Math.abs(System.currentTimeMillis() - millis);
            if (drift > toleranceSeconds * 1000) {
                return reject(response, "时间戳已过期（偏差 " + (drift / 1000) + " 秒 > 容差 " + toleranceSeconds + " 秒）");
            }
        }

        // ② 取密钥（external_channel_config.app_secret）
        Map<String, Object> row = channelConfigMapper.selectSecretByAppId(appId.trim());
        String secret = row == null ? null : (String) row.get("appSecret");
        if (isBlank(secret)) {
            log.warn("[开放API] 拒绝调用：appId={} 未在 external_channel_config 配置启用的对接密钥，path={}", appId, path);
            return reject(response, "未配置该 appId 的对接密钥（渠道配置缺失或已停用）");
        }

        // ③ 验签（恒定时间比较，防时序侧信道）
        String expected = sign(appId.trim(), timestamp.trim(), path, secret);
        if (!MessageDigest.isEqual(
                expected.toLowerCase().getBytes(StandardCharsets.UTF_8),
                sign.trim().toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            log.warn("[开放API] 拒绝调用：签名不匹配 appId={} path={}", appId, path);
            return reject(response, "签名不匹配");
        }
        return true;
    }

    /**
     * 签名口径：{@code HEX(HMAC-SHA256(appSecret, appId + timestamp + requestURI))}。
     *
     * <p>与 `ChannelOrderService.sign` 同一套写法（HEX 小写、字段拼接 null 视为空串），
     * 便于接入方复用同一份签名代码。</p>
     */
    public static String sign(String appId, String timestamp, String requestUri, String secret) throws Exception {
        String payload = (appId == null ? "" : appId)
                + (timestamp == null ? "" : timestamp)
                + (requestUri == null ? "" : requestUri);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private boolean reject(HttpServletResponse response, String reason) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":401,\"message\":\"" + reason + "\",\"data\":null}");
        return false;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
