package cn.aiedge.devtool.service;

import cn.aiedge.devtool.config.ApiTestProperties;
import cn.aiedge.devtool.dto.ApiTestSendRequest;
import cn.aiedge.devtool.dto.ApiTestSendResult;
import cn.aiedge.devtool.security.ApiTestTargetGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * API 测试台的**受控出站请求**执行器。
 *
 * <p>本类只做一件事：把使用者显式填写的请求，经由服务端已校验的通道发出去，并把真实结果带回来。
 * 它**不**代理浏览器的会话，也**不**复制入站 HTTP 请求的任何头 —— 平台凭据在这里被物理性地剥离
 * （没有任何一行代码把 {@code HttpServletRequest} 的头放进出站请求）。
 *
 * <p><b>本系统安全设计（非业界标准）</b>：OWASP《SSRF Prevention Cheat Sheet》没有针对
 * 「API 调试台」的章节，「剥离平台凭据 + 响应大小/超时上限」是本系统自建口径（D 级），
 * 不声称业界标准；协议白名单 / allowlist / 禁跳转 / 屏蔽内网网段 四项才有 OWASP 依据。
 *
 * <p>逐条闸门落点：
 * <ol>
 *   <li>协议白名单 → {@link ApiTestTargetGuard#validate}；</li>
 *   <li>目标 allowlist（服务端配置）→ {@link ApiTestTargetGuard#validate}；</li>
 *   <li>屏蔽内网网段 → {@link ApiTestTargetGuard#validate}；</li>
 *   <li>防 DNS Rebinding → {@link ApiTestTargetGuard#pinnedDnsResolver} + 本类的连接管理器；</li>
 *   <li>禁跟随重定向 → {@code disableRedirectHandling()} + {@code setRedirectsEnabled(false)}；</li>
 *   <li>超时（连接 + 读取）→ {@link RequestConfig}；</li>
 *   <li>响应体上限（流式截断）→ {@link #readCapped}；</li>
 *   <li>剥离平台凭据 + 逐跳头丢弃 → {@link #validatedHeaders}。</li>
 * </ol>
 *
 * @since 2026-09-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiTestOutboundService {

    /** 方法白名单（与页面下拉一致 + HEAD） */
    private static final Set<String> ALLOWED_METHODS = Set.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE");
    /** 不携带请求体的方法 */
    private static final Set<String> BODYLESS_METHODS = Set.of("GET", "HEAD");
    /** RFC 7230 token：请求头名称必须完全由这些字符构成，否则可能存在头注入 */
    private static final Pattern HEADER_NAME_PATTERN = Pattern.compile("^[!#$%&'*+\\-.^_`|~0-9A-Za-z]+$");

    /**
     * 逐跳（hop-by-hop）与平台内部头黑名单 —— 即使使用者显式填写也**丢弃/拒绝**。
     * <p>逐跳头：Host / Content-Length / Connection / Transfer-Encoding / Upgrade / TE / Trailer / Expect / Proxy-*
     * （由 HTTP 客户端自行计算，使用者指定会造成请求走私与长度混淆）。
     * <p>平台内部头：Cookie / Set-Cookie / X-Tenant-Id / tenantId / X-User-Id / X-Forwarded-* / X-Real-IP
     * （伪造这些头等于伪造身份或来源，且与「凭据剥离」直接冲突）。
     */
    private static final Set<String> BLOCKED_HEADERS = Set.of(
            "host", "content-length", "connection", "transfer-encoding", "upgrade", "te", "trailer", "expect",
            "keep-alive", "proxy-connection", "proxy-authorization",
            "cookie", "cookie2", "set-cookie", "set-cookie2",
            "x-tenant-id", "tenantid", "x-user-id", "x-userid", "x-forwarded-for", "x-forwarded-host",
            "x-forwarded-proto", "x-forwarded-port", "x-real-ip", "x-original-ip", "x-client-ip",
            "sa-token", "sa-token-login-id"
    );
    /** 响应头里属于凭据/挑战语义的，回显时脱敏 */
    private static final Set<String> REDACTED_RESPONSE_HEADERS = Set.of(
            "set-cookie", "set-cookie2", "www-authenticate", "proxy-authenticate", "authorization", "x-auth-token"
    );

    private final ApiTestProperties props;
    private final ApiTestTargetGuard guard;

    // ═══════════════════════════════════════════════════════════════
    // 主入口
    // ═══════════════════════════════════════════════════════════════

    public ApiTestSendResult send(ApiTestSendRequest request) {
        if (request == null) {
            return ApiTestSendResult.rejected("请求参数不能为空", null);
        }

        // ── 1) 方法白名单 ──
        String method = request.getMethod() == null ? "" : request.getMethod().trim().toUpperCase(Locale.ROOT);
        if (method.isEmpty()) {
            return ApiTestSendResult.rejected("请选择请求方法（" + String.join(" / ", ALLOWED_METHODS) + "）", request.getUrl());
        }
        if (!ALLOWED_METHODS.contains(method)) {
            return ApiTestSendResult.rejected("不支持的请求方法「" + method + "」：只允许 " + String.join(" / ", ALLOWED_METHODS),
                    request.getUrl());
        }

        // ── 2) 请求体 ──
        String body = request.getBody();
        if (body != null && !body.isEmpty()) {
            if (BODYLESS_METHODS.contains(method)) {
                return ApiTestSendResult.rejected(method + " 请求不允许携带请求体", request.getUrl());
            }
            int bodyBytes = body.getBytes(StandardCharsets.UTF_8).length;
            if (bodyBytes > props.getMaxRequestBodyBytes()) {
                return ApiTestSendResult.rejected("请求体过大（" + bodyBytes + " 字节，上限 "
                        + props.getMaxRequestBodyBytes() + " 字节）", request.getUrl());
            }
        } else {
            body = null;
        }

        // ── 3) 请求头：逐跳头 / 平台内部头丢弃；其余按显式填写放行 ──
        Map<String, String> headers = new LinkedHashMap<>();
        String headerError = validatedHeaders(request.getHeaders(), headers);
        if (headerError != null) {
            return ApiTestSendResult.rejected(headerError, request.getUrl());
        }

        // ── 4~7) 目标闸门（协议 / allowlist / 内网 / DNS Rebinding 钉 IP）──
        ApiTestTargetGuard.Decision decision = guard.validate(request.getUrl());
        if (!decision.allowed()) {
            log.warn("[API测试台] 出站请求被闸门拒绝: url={} reason={}", request.getUrl(), decision.reason());
            return ApiTestSendResult.rejected(decision.reason(), request.getUrl());
        }
        ApiTestTargetGuard.GuardedTarget target = decision.target();

        return execute(target, method, headers, body);
    }

    // ═══════════════════════════════════════════════════════════════
    // 请求头校验（闸门 8）
    // ═══════════════════════════════════════════════════════════════

    /**
     * 校验并收敛使用者显式填写的请求头。
     *
     * <p>注意：本方法的输入**只**来自请求体的 {@code headers} 字段 ——
     * 入站 HTTP 请求自身的头（含 Authorization / Cookie / Sa-Token）从不参与，
     * 因此「平台凭据被转发到目标」在结构上不可能发生。
     *
     * @return null 表示通过；否则返回可直接展示的拒绝原因
     */
    private String validatedHeaders(Map<String, String> input, Map<String, String> out) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        if (input.size() > props.getMaxHeaders()) {
            return "请求头过多（" + input.size() + " 个，上限 " + props.getMaxHeaders() + " 个）";
        }
        Set<String> seen = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            String rawName = entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue();
            if (rawName == null || rawName.isBlank()) {
                continue;
            }
            String name = rawName.trim();
            if (!HEADER_NAME_PATTERN.matcher(name).matches()) {
                return "请求头名称「" + name + "」含非法字符（仅允许 RFC 7230 token 字符）";
            }
            String lower = name.toLowerCase(Locale.ROOT);
            if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
                return "请求头「" + name + "」的值不能包含换行符（防请求头注入）";
            }
            if (BLOCKED_HEADERS.contains(lower)) {
                return "禁止设置请求头「" + name + "」：逐跳头（Host/Content-Length/Connection/Transfer-Encoding 等）"
                        + "与平台内部头（Cookie/X-Tenant-Id/X-Forwarded-* 等）一律丢弃，防请求头注入与身份伪造";
            }
            if (lower.startsWith("proxy-")) {
                return "禁止设置请求头「" + name + "」：代理控制类头不可由调用方注入";
            }
            if ("authorization".equals(lower) && !props.isAllowUserAuthorization()) {
                return "禁止设置请求头「Authorization」：本系统不代为转发会话凭据（凭据剥离）。"
                        + "如确需调试需鉴权接口，请由运维开启 app.api-test.allow-user-authorization 后自行粘贴凭据";
            }
            if (value.length() > props.getMaxHeaderValueLength()) {
                return "请求头「" + name + "」的值过长（上限 " + props.getMaxHeaderValueLength() + " 字符）";
            }
            if (!seen.add(lower)) {
                return "请求头「" + name + "」重复出现";
            }
            out.put(name, value);
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════
    // 执行（闸门 5/6/7）
    // ═══════════════════════════════════════════════════════════════

    private ApiTestSendResult execute(ApiTestTargetGuard.GuardedTarget target,
                                      String method,
                                      Map<String, String> headers,
                                      String body) {
        URI uri = target.uri();
        String displayUrl = target.scheme() + "://" + target.host() + ":" + target.port() + uri.getRawPath()
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        String targetUrl = uri.toASCIIString();

        // 闸门 6：连接超时 + 读取超时（两者都设，避免对端「挂着不吐数据」占死线程）
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(props.getConnectTimeoutMs())
                .setConnectionRequestTimeout(props.getConnectTimeoutMs())
                .setSocketTimeout(props.getReadTimeoutMs())
                // 闸门 5：禁跟随重定向（302 到内网是经典绕过手法）
                .setRedirectsEnabled(false)
                .setMaxRedirects(0)
                .build();

        // 闸门 4：把已校验的 IP 钉进连接层 —— 客户端不会、也不能重新解析域名
        // （4.5.x 的 PoolingHttpClientConnectionManager 没有 Builder，故用带 DnsResolver 的构造器。）
        //
        // ⚠️ 第一个参数**必须**是真实的 socket 工厂注册表，**不能传 null**：
        //    传 null 时构造能通过，但**发起请求**会抛
        //    `IllegalArgumentException: Socket factory registry may not be null`
        //    （实踩：2026-09-19 真机验证时，同源 /api/auth/check 请求报 400 该消息，
        //      而"被拒绝"的用例全都能过 —— 因为它们在建连接之前就被闸门挡下了，
        //      于是这个缺陷被"拒绝路径全绿"掩盖）。
        Registry<ConnectionSocketFactory> socketRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();
        PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager(socketRegistry, guard.pinnedDnsResolver(target));
        connectionManager.setMaxTotal(2);
        connectionManager.setDefaultMaxPerRoute(2);

        long startedAt = System.currentTimeMillis();
        try (CloseableHttpClient client = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .disableRedirectHandling()
                .disableAutomaticRetries()
                .disableCookieManagement()
                .setUserAgent("AI-Ready-APITest/1.0 (server-side controlled proxy)")
                .build()) {

            HttpRequestBase httpRequest = buildRequest(target, method, body);
            for (Map.Entry<String, String> header : headers.entrySet()) {
                if ("content-type".equalsIgnoreCase(header.getKey()) && body != null) {
                    continue; // Content-Type 已由实体携带，避免出现两个冲突值
                }
                httpRequest.setHeader(header.getKey(), header.getValue());
            }

            try (CloseableHttpResponse response = client.execute(httpRequest)) {
                int status = response.getStatusLine().getStatusCode();
                Map<String, String> responseHeaders = sanitizeResponseHeaders(response.getAllHeaders());

                byte[] bytes = new byte[0];
                Charset charset = StandardCharsets.UTF_8;
                boolean truncated = false;
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    ContentType contentType = ContentType.get(entity);
                    if (contentType != null && contentType.getCharset() != null) {
                        charset = contentType.getCharset();
                    }
                    try (InputStream in = entity.getContent()) {
                        CappedRead capped = readCapped(in, props.getMaxResponseBytes());
                        bytes = capped.bytes();
                        truncated = capped.truncated();
                    }
                }
                long elapsedMs = System.currentTimeMillis() - startedAt;

                boolean redirectBlocked = status >= 300 && status < 400;
                String text = new String(bytes, charset);
                String message;
                if (redirectBlocked) {
                    message = "目标返回了重定向（HTTP " + status + "）：已按安全策略**阻止跟随**，未向跳转目标发出第二个请求"
                            + (responseHeaders.getOrDefault("Location", "").isEmpty()
                                    ? "" : "（Location: " + responseHeaders.get("Location") + "）");
                } else if (truncated) {
                    message = "请求已完成（HTTP " + status + "，耗时 " + elapsedMs + "ms），"
                            + "但响应体超过 " + (props.getMaxResponseBytes() / 1024) + "KB 上限，已流式截断";
                } else if (status >= 200 && status < 300) {
                    message = "请求成功（HTTP " + status + "，耗时 " + elapsedMs + "ms）";
                } else {
                    message = "服务端返回 HTTP " + status + "（耗时 " + elapsedMs + "ms）：这是目标服务端的真实结论，响应体已原样展示";
                }
                log.info("[API测试台] 出站完成: {} {} → HTTP {} ({}ms, {}B, truncated={}, redirectBlocked={})",
                        method, displayUrl, status, elapsedMs, bytes.length, truncated, redirectBlocked);
                return new ApiTestSendResult(true, message, null, displayUrl, status, elapsedMs,
                        bytes.length, truncated, redirectBlocked, responseHeaders, text);
            }
        } catch (ConnectTimeoutException e) {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            log.warn("[API测试台] 连接超时: {} {}（{}ms）", method, displayUrl, elapsedMs);
            return ApiTestSendResult.failed("连接超时：超过 " + props.getConnectTimeoutMs() / 1000 + " 秒未能建立连接", displayUrl, elapsedMs);
        } catch (SocketTimeoutException e) {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            log.warn("[API测试台] 读取超时: {} {}（{}ms）", method, displayUrl, elapsedMs);
            return ApiTestSendResult.failed("读取超时：超过 " + props.getReadTimeoutMs() / 1000 + " 秒未读到完整响应", displayUrl, elapsedMs);
        } catch (UnknownHostException e) {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            log.warn("[API测试台] 域名解析失败: {} {} - {}", method, displayUrl, e.getMessage());
            return ApiTestSendResult.failed("域名无法解析：" + target.host(), displayUrl, elapsedMs);
        } catch (IOException e) {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            log.warn("[API测试台] 出站请求未完成: {} {} - {}", method, displayUrl, e.getMessage());
            return ApiTestSendResult.failed("请求未能发出或未完成：" + e.getClass().getSimpleName()
                    + (e.getMessage() == null ? "" : " - " + e.getMessage()), displayUrl, elapsedMs);
        } catch (RuntimeException e) {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            log.error("[API测试台] 出站请求异常: {} {}", method, displayUrl, e);
            return ApiTestSendResult.failed("请求处理异常：" + e.getClass().getSimpleName(), displayUrl, elapsedMs);
        }
    }

    private HttpRequestBase buildRequest(ApiTestTargetGuard.GuardedTarget target, String method, String body) {
        URI uri = target.uri();
        boolean withBody = body != null && !BODYLESS_METHODS.contains(method);
        HttpRequestBase request = switch (method) {
            case "GET" -> new HttpGet(uri);
            case "HEAD" -> new HttpHead(uri);
            case "POST" -> new HttpPost(uri);
            case "PUT" -> new HttpPut(uri);
            case "PATCH" -> new HttpPatch(uri);
            case "DELETE" -> withBody ? new EntityRequest(uri, "DELETE") : new HttpDelete(uri);
            default -> throw new IllegalStateException("未支持的方法: " + method);
        };
        if (withBody && request instanceof HttpEntityEnclosingRequestBase enclosing) {
            enclosing.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
        }
        return request;
    }

    /** 带请求体的自定义方法请求（Apache HttpClient 的 HttpDelete 不支持实体） */
    private static final class EntityRequest extends HttpEntityEnclosingRequestBase {

        private final String methodName;

        private EntityRequest(URI uri, String methodName) {
            setURI(uri);
            this.methodName = methodName;
        }

        @Override
        public String getMethod() {
            return methodName;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 响应处理（闸门 7：流式截断；响应头脱敏）
    // ═══════════════════════════════════════════════════════════════

    private record CappedRead(byte[] bytes, boolean truncated) {
    }

    /**
     * **流式**读取响应体，最多保留 maxBytes 字节。
     *
     * <p>刻意不用 {@code EntityUtils.toByteArray} —— 那会先把整个响应读进内存再截断，
     * 遇到「返回 2GB 的端点」时服务端先 OOM。这里读满上限就停，剩下的不读。
     *
     * <p>截断判定：读满上限即认为「已截断 / 可能已截断」（宁可多报一次，也不静默丢数据）。
     */
    private static CappedRead readCapped(InputStream in, int maxBytes) throws IOException {
        if (maxBytes <= 0) {
            return new CappedRead(new byte[0], true);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(maxBytes, 64 * 1024));
        byte[] buffer = new byte[8192];
        int total = 0;
        while (total < maxBytes) {
            int want = Math.min(buffer.length, maxBytes - total);
            int read = in.read(buffer, 0, want);
            if (read == -1) {
                break;
            }
            out.write(buffer, 0, read);
            total += read;
        }
        return new CappedRead(out.toByteArray(), total >= maxBytes);
    }

    private Map<String, String> sanitizeResponseHeaders(Header[] headers) {
        Map<String, String> out = new LinkedHashMap<>();
        if (headers == null) {
            return out;
        }
        for (Header header : headers) {
            if (out.size() >= props.getMaxResponseHeaders()) {
                break;
            }
            String name = header.getName();
            if (name == null || name.isBlank()) {
                continue;
            }
            String lower = name.toLowerCase(Locale.ROOT);
            if (REDACTED_RESPONSE_HEADERS.contains(lower)) {
                // 响应侧凭据同样不回显 —— 调试台没有理由把 Set-Cookie 原样搬到页面上
                out.putIfAbsent(name, "***（含凭据语义，已脱敏）");
                continue;
            }
            String value = header.getValue() == null ? "" : header.getValue();
            if (value.length() > props.getMaxResponseHeaderValueLength()) {
                value = value.substring(0, props.getMaxResponseHeaderValueLength()) + "…（已截断）";
            }
            out.putIfAbsent(name, value);
        }
        return out;
    }

    // ═══════════════════════════════════════════════════════════════
    // 供控制器做前置说明用
    // ═══════════════════════════════════════════════════════════════

    /** 当前生效的安全边界（页面据此如实展示，不做前端臆测） */
    public Map<String, Object> currentPolicy() {
        List<String> blockedRanges = new ArrayList<>(List.of(
                "0.0.0.0/8", "127.0.0.0/8", "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16",
                "169.254.0.0/16", "100.64.0.0/10", "192.0.0.0/24", "224.0.0.0/4", "240.0.0.0/4",
                "::1", "fc00::/7", "fe80::/10", "ff00::/8"));
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("enabled", props.isEnabled());
        policy.put("selfBaseUrl", props.getSelfBaseUrl());
        policy.put("allowExternal", props.isAllowExternal());
        policy.put("allowedHosts", props.getAllowedHosts());
        policy.put("allowUserAuthorization", props.isAllowUserAuthorization());
        policy.put("exemptSelfOriginFromPrivateIpCheck", props.isExemptSelfOriginFromPrivateIpCheck());
        policy.put("blockedIpRanges", blockedRanges);
        policy.put("blockedHeaders", new ArrayList<>(BLOCKED_HEADERS));
        policy.put("allowedMethods", new ArrayList<>(ALLOWED_METHODS));
        policy.put("connectTimeoutMs", props.getConnectTimeoutMs());
        policy.put("readTimeoutMs", props.getReadTimeoutMs());
        policy.put("maxResponseBytes", props.getMaxResponseBytes());
        policy.put("maxRequestBodyBytes", props.getMaxRequestBodyBytes());
        policy.put("rateLimitPerMinute", props.getRateLimitPerMinute());
        policy.put("followRedirects", false);
        policy.put("pinResolvedIp", true);
        return policy;
    }
}
