package cn.aiedge.devtool.security;

import cn.aiedge.devtool.config.ApiTestProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.DnsResolver;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * API 测试台出站目标的**安全闸门**（本系统的 SSRF 兜底）。
 *
 * <p>这是本页唯一决定「一个请求能不能发出去」的地方。前端做过的校验（同源 /api/ 前缀、
 * 禁重定向、超时、响应上限）在这里**再做一遍**，因为前端校验可以被打断点、改 JS、
 * 或直接用开发者工具绕过 —— 服务端必须独立成立。
 *
 * <p><b>逐条闸门与依据</b>（OWASP《Server-Side Request Forgery Prevention Cheat Sheet》有覆盖的部分
 * 标注为「OWASP 依据」；「调试台剥离凭据 / 响应大小上限」OWASP **没有**对应章节，
 * 属**本系统自建安全设计**，不声称业界标准）：
 * <ol>
 *   <li><b>协议白名单</b>（OWASP 依据：URL scheme allowlist，禁用 {@code file://} 等）—— {@link #validate};</li>
 *   <li><b>目标 allowlist</b>（OWASP 依据：allowlist over denylist）—— 默认只放行 {@code app.api-test.self-base-url}
 *       的同源地址；外部地址必须来自服务端配置 {@code app.api-test.allowed-hosts}，
 *       **绝不接受前端传入 allowlist**；</li>
 *   <li><b>屏蔽内网网段</b>（OWASP 依据：deny private/link-local/loopback ranges）—— {@link #describeBlockedAddress};</li>
 *   <li><b>防 DNS Rebinding</b>（OWASP 依据：解析一次、按解析结果连接）—— {@link #pinnedDnsResolver}
 *       —— 先解析拿到 IP 并校验，再把**同一个 IP** 钉进连接层，杜绝「校验用域名、连接时重解析」的空窗；</li>
 *   <li><b>禁跟随重定向</b>（OWASP 依据：disable redirect following）—— 落在出站服务（{@code disableRedirectHandling}）；</li>
 *   <li><b>超时</b>—— 落在出站服务（连接 + 读取各上限）；</li>
 *   <li><b>响应体上限</b>—— 落在出站服务（流式截断）；</li>
 *   <li><b>剥离平台凭据 + 逐跳头丢弃</b>—— 落在出站服务（本类不接触入站请求头）。</li>
 * </ol>
 *
 * @since 2026-09-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiTestTargetGuard {

    private final ApiTestProperties props;

    /** 已通过全部闸门的目标：**pinnedAddress 是校验过的那一个 IP**，连接层只允许用它 */
    public record GuardedTarget(URI uri,
                                String host,
                                int port,
                                String scheme,
                                InetAddress pinnedAddress,
                                boolean selfOrigin) {
    }

    /** 闸门结论：拒绝时 reason 是**可直接展示给使用者**的中文原因 */
    public record Decision(boolean allowed, String reason, GuardedTarget target) {

        public static Decision allow(GuardedTarget target) {
            return new Decision(true, null, target);
        }

        public static Decision reject(String reason) {
            return new Decision(false, reason, null);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 闸门 1~4：解析 + 协议白名单 + allowlist + 内网屏蔽
    // ═══════════════════════════════════════════════════════════════

    public Decision validate(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return Decision.reject("请求地址不能为空");
        }
        String raw = rawInput.trim();
        if (raw.length() > props.getMaxUrlLength()) {
            return Decision.reject("请求地址过长（上限 " + props.getMaxUrlLength() + " 字符）");
        }
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c < 0x20 || c == 0x7f) {
                return Decision.reject("请求地址不能包含控制字符（含制表/换行）");
            }
        }
        if (raw.indexOf(' ') >= 0 || raw.indexOf('\\') >= 0) {
            return Decision.reject("请求地址不能包含空格或反斜杠");
        }

        URI selfBase = parseSelfBase();
        URI target;
        boolean selfOrigin;

        if (raw.startsWith("/")) {
            // ── 相对路径：拼到服务端配置的「本系统自身 origin」之后 ──
            if (raw.startsWith("//")) {
                return Decision.reject("禁止协议相对地址（//host/...）：它会把请求打到任意主机。只允许以 /api/ 开头的同源相对路径，或完整的 http(s) URL");
            }
            if (!raw.startsWith("/api/")) {
                return Decision.reject("同源相对路径必须以 /api/ 开头（本端点是本系统 API 调试台，不放行 /doc.html 等非 API 路径）");
            }
            if (selfBase == null) {
                return Decision.reject("服务端未配置 app.api-test.self-base-url，无法解析相对路径；请让运维补上配置，或改填完整的 http(s) URL");
            }
            try {
                target = new URI(selfBase.toASCIIString() + raw);
            } catch (URISyntaxException e) {
                return Decision.reject("请求地址不是合法的 URL：" + e.getReason());
            }
            selfOrigin = true;
        } else {
            // ── 绝对 URL ──
            try {
                target = new URI(raw);
            } catch (URISyntaxException e) {
                return Decision.reject("请求地址不是合法的 URL：" + e.getReason());
            }
            String rawScheme = target.getScheme();
            if (rawScheme == null || rawScheme.isBlank()) {
                return Decision.reject("请求地址缺少协议头：只允许 http:// 或 https:// 开头的完整 URL");
            }
            String scheme = rawScheme.toLowerCase(Locale.ROOT);
            // 闸门 1：协议白名单
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                return Decision.reject("只允许 http/https 协议，已拒绝 \"" + scheme + ":\""
                        + "（file: / ftp: / gopher: / jar: / data: / dict: / ldap: / tftp: 等协议一律禁止）");
            }
            if (target.getUserInfo() != null) {
                return Decision.reject("URL 中禁止携带 userinfo（形如 http://user:pass@host/）—— 它常用于混淆真实目标主机");
            }
            if (target.getHost() == null || target.getHost().isBlank()) {
                return Decision.reject("URL 缺少主机名");
            }
            selfOrigin = selfBase != null && sameOrigin(target, selfBase);
        }

        String scheme = target.getScheme().toLowerCase(Locale.ROOT);
        String host = stripBrackets(target.getHost()).toLowerCase(Locale.ROOT);
        int port = effectivePort(target, scheme);

        // 端口值域：负数 / 0 / 超 65535 都是畸形输入，直接拒绝（别让它走到连接层变成奇怪报错）
        if (port <= 0 || port > 65535) {
            return Decision.reject("非法的端口号：" + port + "（有效范围 1–65535）");
        }

        // 上级目录跳转（`..` 与 %2e%2e —— URI#getPath 会先做百分号解码，故两种写法都能拦住）
        if (hasDotDotSegment(target)) {
            return Decision.reject("请求地址不能包含 \"..\" 上级目录跳转");
        }

        // ── 闸门 2：目标 allowlist（只认服务端配置） ──
        if (!selfOrigin) {
            if (!props.isAllowExternal()) {
                return Decision.reject("目标 " + scheme + "://" + host + ":" + port
                        + " 不在允许范围内：本端点默认只允许打**本系统自身**（同源 "
                        + (selfBase == null ? "（未配置）" : selfBase) + "）。"
                        + "如需放行外部地址，必须由服务端配置 app.api-test.allowed-hosts 白名单并开启 allow-external —— 本端点不接受前端传入的白名单。");
            }
            if (!hostAllowed(host, port)) {
                return Decision.reject("目标主机 " + host + " 不在服务端白名单 app.api-test.allowed-hosts 内");
            }
        }

        // ── 闸门 3 + 4：解析一次，按解析结果校验并「钉住」 ──
        InetAddress[] resolved;
        try {
            resolved = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            return Decision.reject("域名无法解析：" + host);
        }
        if (resolved == null || resolved.length == 0) {
            return Decision.reject("域名无法解析：" + host);
        }

        // 同源豁免只覆盖「内网网段」这一类；0.0.0.0 / 组播 / 广播任何时候都拒绝
        boolean exemptSelfOrigin = selfOrigin && props.isExemptSelfOriginFromPrivateIpCheck();
        for (InetAddress address : resolved) {
            if (isNeverAllowed(address)) {
                return Decision.reject("目标解析到不可用地址 " + address.getHostAddress()
                        + "（0.0.0.0 / 组播 / 广播地址），已拒绝");
            }
            String blocked = describeBlockedAddress(address);
            if (blocked != null && !exemptSelfOrigin) {
                return Decision.reject("目标解析到内网/本机地址 " + address.getHostAddress() + "，已按 SSRF 防护策略拒绝"
                        + "（屏蔽 " + blocked + "）。"
                        + (selfOrigin ? "" : "如需调试内网服务，请由运维通过服务端配置显式放行。"));
            }
        }

        InetAddress pinned = resolved[0];
        log.debug("[API测试台] 闸门通过: {} {}://{}:{} → pinned={} selfOrigin={}", rawInput, scheme, host, port,
                pinned.getHostAddress(), selfOrigin);
        return Decision.allow(new GuardedTarget(target, host, port, scheme, pinned, selfOrigin));
    }

    // ═══════════════════════════════════════════════════════════════
    // 闸门 4：DNS Rebinding 防护 —— 把已校验的 IP 钉进连接层
    // ═══════════════════════════════════════════════════════════════

    /**
     * 返回一个「只会返回已校验 IP」的解析器，交给连接管理器。
     *
     * <p>这样 HTTP 客户端**没有机会**在连接时重新解析域名 —— 攻击者即使把 DNS 记录
     * 在「我们校验」与「客户端连接」之间改成 169.254.169.254，连接仍然只会打到
     * 校验通过的那个 IP。<b>不要**改成「先校验域名、再让客户端自己解析一遍」的写法。
     */
    public DnsResolver pinnedDnsResolver(GuardedTarget target) {
        return host -> {
            if (host == null || !host.equalsIgnoreCase(target.host())) {
                // 理论上不会发生（一个请求一个目标）；真发生了说明有非预期的主机被解析，
                // 宁可失败也不要放行 —— 防的是「静默改道」。
                throw new UnknownHostException("已拒绝：解析目标与已校验主机不一致（" + host + "）");
            }
            return new InetAddress[] {target.pinnedAddress()};
        };
    }

    // ═══════════════════════════════════════════════════════════════
    // 内网 / 本机网段判定
    // ═══════════════════════════════════════════════════════════════

    /**
     * 永不放行的地址（即使命中同源豁免）：0.0.0.0/:: 与组播、广播。
     */
    public static boolean isNeverAllowed(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isMulticastAddress()
                || isBroadcast(address);
    }

    /**
     * 命中内网/本机网段时返回具体网段名（用于给使用者看的原因），否则返回 null。
     *
     * <p>覆盖：{@code 0.0.0.0/8}、{@code 127.0.0.0/8}、{@code 10.0.0.0/8}、
     * {@code 172.16.0.0/12}、{@code 192.168.0.0/16}、{@code 169.254.0.0/16}（含云元数据 169.254.169.254）、
     * {@code 100.64.0.0/10}、{@code 192.0.0.0/24}、{@code 224.0.0.0/4}、{@code 240.0.0.0/4}、
     * {@code ::1}、{@code fc00::/7}、{@code fe80::/10}、IPv4-mapped IPv6。
     */
    public static String describeBlockedAddress(InetAddress address) {
        byte[] raw = address.getAddress();
        if (raw.length == 4) {
            return describeBlockedV4(raw);
        }
        if (raw.length == 16) {
            if (isIpv4MappedV6(raw)) {
                // ::ffff:a.b.c.d —— 按内层 IPv4 判定（否则可绕过）
                return describeBlockedV4(new byte[] {raw[12], raw[13], raw[14], raw[15]});
            }
            if (isAllZero(raw, 0, 15) && raw[15] == 1) {
                return "::1 本机回环";
            }
            if (isAllZero(raw, 0, 16)) {
                return ":: 未指定地址";
            }
            if ((raw[0] & 0xFE) == 0xFC) { // fc00::/7
                return "fc00::/7（IPv6 唯一本地地址）";
            }
            if ((raw[0] & 0xFF) == 0xFE && (raw[1] & 0xC0) == 0x80) { // fe80::/10
                return "fe80::/10（IPv6 链路本地）";
            }
            if ((raw[0] & 0xFF) == 0xFF) { // ff00::/8 组播
                return "ff00::/8（IPv6 组播）";
            }
            return null;
        }
        return null;
    }

    private static String describeBlockedV4(byte[] ip) {
        int b0 = ip[0] & 0xFF;
        int b1 = ip[1] & 0xFF;
        int b2 = ip[2] & 0xFF;
        int b3 = ip[3] & 0xFF;
        if (b0 == 0) {
            return "0.0.0.0/8（本网络）";
        }
        if (b0 == 127) {
            return "127.0.0.0/8（本机回环）";
        }
        if (b0 == 10) {
            return "10.0.0.0/8（内网）";
        }
        if (b0 == 172 && (b1 & 0xF0) == 16) {
            return "172.16.0.0/12（内网）";
        }
        if (b0 == 192 && b1 == 168) {
            return "192.168.0.0/16（内网）";
        }
        if (b0 == 169 && b1 == 254) {
            return "169.254.0.0/16（链路本地，含云元数据 169.254.169.254）";
        }
        if (b0 == 100 && (b1 & 0xC0) == 64) {
            return "100.64.0.0/10（运营商级 NAT）";
        }
        if (b0 == 192 && b1 == 0 && b2 == 0) {
            return "192.0.0.0/24（IETF 协议保留）";
        }
        if ((b0 & 0xF0) == 224) {
            return "224.0.0.0/4（组播）";
        }
        if ((b0 & 0xF0) == 240) {
            return "240.0.0.0/4（保留）";
        }
        if (b0 == 255 && b1 == 255 && b2 == 255 && b3 == 255) {
            return "255.255.255.255（广播）";
        }
        return null;
    }

    private static boolean isBroadcast(InetAddress address) {
        byte[] raw = address.getAddress();
        if (raw.length != 4) {
            return false;
        }
        for (byte b : raw) {
            if ((b & 0xFF) != 0xFF) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIpv4MappedV6(byte[] raw) {
        for (int i = 0; i < 10; i++) {
            if (raw[i] != 0) {
                return false;
            }
        }
        return (raw[10] & 0xFF) == 0xFF && (raw[11] & 0xFF) == 0xFF;
    }

    private static boolean isAllZero(byte[] raw, int from, int to) {
        for (int i = from; i < to; i++) {
            if (raw[i] != 0) {
                return false;
            }
        }
        return true;
    }

    // ═══════════════════════════════════════════════════════════════
    // 工具
    // ═══════════════════════════════════════════════════════════════

    /** 解析 {@code app.api-test.self-base-url}；未配置或非法时返回 null（调用方据此给出明确原因） */
    public URI parseSelfBase() {
        String configured = props.getSelfBaseUrl();
        if (configured == null || configured.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(configured.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if ((!"http".equals(scheme) && !"https".equals(scheme)) || uri.getHost() == null) {
                log.warn("[API测试台] app.api-test.self-base-url 配置非法（必须是 http(s)://host[:port]）：{}", configured);
                return null;
            }
            int port = effectivePort(uri, scheme);
            return new URI(scheme + "://" + stripBrackets(uri.getHost()).toLowerCase(Locale.ROOT) + ":" + port);
        } catch (URISyntaxException e) {
            log.warn("[API测试台] app.api-test.self-base-url 无法解析：{}（{}）", configured, e.getReason());
            return null;
        }
    }

    private static int effectivePort(URI uri, String scheme) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        return "https".equals(scheme) ? 443 : 80;
    }

    private static String stripBrackets(String host) {
        if (host == null) {
            return "";
        }
        if (host.startsWith("[") && host.endsWith("]") && host.length() > 2) {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }

    /** 两个 URI 是否同 origin（scheme + host + 有效端口） */
    public static boolean sameOrigin(URI a, URI b) {
        String sa = a.getScheme() == null ? "" : a.getScheme().toLowerCase(Locale.ROOT);
        String sb = b.getScheme() == null ? "" : b.getScheme().toLowerCase(Locale.ROOT);
        if (!sa.equals(sb) || sa.isEmpty()) {
            return false;
        }
        String ha = stripBrackets(a.getHost()).toLowerCase(Locale.ROOT);
        String hb = stripBrackets(b.getHost()).toLowerCase(Locale.ROOT);
        if (!ha.equals(hb) || ha.isEmpty()) {
            return false;
        }
        return effectivePort(a, sa) == effectivePort(b, sb);
    }

    /**
     * 主机是否命中服务端白名单。
     * <p>条目形态：
     * <ul>
     *   <li>{@code example.com} —— 仅主机，任意端口；</li>
     *   <li>{@code example.com:8443} —— 主机 + 端口（两者都要匹配）；</li>
     *   <li>{@code *.example.com} —— 后缀通配，匹配 {@code a.example.com}，**不**匹配 {@code example.com} 本身；</li>
     *   <li>{@code [::1]} / {@code ::1} —— IPv6 字面量（含冒号，不会被误当成 host:port）。</li>
     * </ul>
     */
    public boolean hostAllowed(String host, int port) {
        if (props.getAllowedHosts() == null) {
            return false;
        }
        for (String entryRaw : props.getAllowedHosts()) {
            if (entryRaw == null || entryRaw.isBlank()) {
                continue;
            }
            String entry = entryRaw.trim().toLowerCase(Locale.ROOT);
            if (entry.startsWith("*.")) {
                String suffix = entry.substring(1); // ".example.com"
                if (host.endsWith(suffix) && host.length() > suffix.length()) {
                    return true;
                }
                continue;
            }
            String plain = stripBrackets(entry);
            // 冒号个数 > 1 的是 IPv6 字面量（含 ::1），整体当主机名，不拆端口
            int firstColon = plain.indexOf(':');
            if (firstColon >= 0 && firstColon == plain.lastIndexOf(':')) {
                String entryHost = plain.substring(0, firstColon);
                String entryPort = plain.substring(firstColon + 1);
                if (entryHost.equals(host) && entryPort.equals(String.valueOf(port))) {
                    return true;
                }
                continue;
            }
            if (plain.equals(host)) {
                return true;
            }
        }
        return false;
    }

    /** 路径中是否存在 {@code ..} 上级目录段（URI#getPath 已做百分号解码） */
    private static boolean hasDotDotSegment(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            return false;
        }
        for (String segment : path.split("/")) {
            if ("..".equals(segment)) {
                return true;
            }
        }
        return false;
    }

}
