package cn.aiedge.devtool.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * API 测试台（系统 → 开发工具 → API测试，菜单 62404）后端安全兜底配置。
 *
 * <p>前缀 {@code app.api-test}。全部字段都有**安全默认值** —— 不在 yml 中显式配置时，
 * 也以「最小放行面」运行：只允许打本系统自身（同源），不允许任何外部地址。
 *
 * <p><b>口径（重要）</b>：请求转发面的三个事实来源全部在**服务端配置**，
 * 绝不接受前端传入：
 * <ol>
 *   <li>{@link #selfBaseUrl} —— 「本系统自己」到底是哪个 origin；</li>
 *   <li>{@link #allowExternal} + {@link #allowedHosts} —— 是否放行外部地址、放行哪些；</li>
 *   <li>{@link #allowUserAuthorization} —— 是否允许使用者自带 Authorization 头。</li>
 * </ol>
 * 前端只能决定「打哪个路径/方法/请求体」，路径还要再被本类的规则过一遍。
 *
 * <p><b>安全设计的定位</b>：OWASP《Server-Side Request Forgery Prevention Cheat Sheet》
 * 并没有针对「API 测试/调试工具」的章节，因此「调试台剥离平台凭据 + 响应大小/超时上限」
 * 属于**本系统自建安全设计**（D 级），**不得写成业界标准**；
 * 而协议白名单 / allowlist / 禁跳转 / 屏蔽内网网段 这几项**是有 OWASP 依据的**
 * （见 {@code ApiTestTargetGuard} 类注释逐条对应）。
 *
 * @since 2026-09-19
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.api-test")
public class ApiTestProperties {

    /** 总开关。关闭后端点直接拒绝（返回 success=false 并给出原因），不发出任何出站请求 */
    private boolean enabled = true;

    /**
     * 「本系统自己的 API」的 origin（{@code scheme://host:port}，不带结尾斜杠、不带路径）。
     * <p>相对路径（如 {@code /api/monitor/health}）会拼接在它之后；
     * 绝对 URL 只有与它**完全同 origin** 时才被认作同源自测。
     * <p><b>为什么不从请求头 Host 推导</b>：Host / X-Forwarded-Host 都是调用方可控的输入，
     * 拿它当 allowlist 等于没有 allowlist。故只认服务端配置。
     */
    private String selfBaseUrl = "";

    /** 是否允许打外部地址（默认 false：只允许同源自测）。开启后仍受 {@link #allowedHosts} 限制 */
    private boolean allowExternal = false;

    /**
     * 外网白名单。条目形态：{@code example.com} / {@code example.com:8443} / {@code *.example.com}（后缀通配）。
     * <p>只在 {@link #allowExternal} = true 时生效；命中白名单也**仍要**过内网网段检查。
     */
    private List<String> allowedHosts = new ArrayList<>();

    /** 连接超时（毫秒） */
    private int connectTimeoutMs = 5000;

    /** 读取超时（毫秒）—— 连接建立后的 socket 读超时，防止对端「挂着不吐数据」拖死线程 */
    private int readTimeoutMs = 5000;

    /** 响应体上限（字节，默认 256KB）。超出即**流式截断**，不会把整个响应读进内存 */
    private int maxResponseBytes = 256 * 1024;

    /** 请求体上限（字节，默认 64KB） */
    private int maxRequestBodyBytes = 64 * 1024;

    /** 目标 URL 最大长度 */
    private int maxUrlLength = 2048;

    /** 自定义请求头条数上限 */
    private int maxHeaders = 20;

    /** 单个请求头值长度上限 */
    private int maxHeaderValueLength = 1024;

    /** 响应头回显条数上限 */
    private int maxResponseHeaders = 40;

    /** 响应头值回显长度上限 */
    private int maxResponseHeaderValueLength = 256;

    /** 单用户限流：每分钟最多发起多少次出站调用（0 表示不限流） */
    private int rateLimitPerMinute = 30;

    /**
     * 是否允许使用者**自行填写** {@code Authorization} 头并通过。
     * <p>默认 <b>false</b>：本系统的会话凭据（Sa-Token 的 token 名就是 {@code Authorization}）
     * 一律不得由本端点为使用者「代持并转发」—— 这是「凭据剥离」闸门的一部分。
     * 需要调试需鉴权接口时，由运维显式开启本开关，使用者自行粘贴凭据，风险自担。
     */
    private boolean allowUserAuthorization = false;

    /**
     * 命中「同源（{@link #selfBaseUrl}）」时是否豁免内网网段检查。
     * <p>默认 true。理由：本系统自身的 API 在开发/内网部署时**本来**就绑定在
     * 127.0.0.1 / 10.x / 192.168.x 上，一律按内网拒绝会让本页完全不可用。
     * <p>豁免范围被严格限定为「origin 与 selfBaseUrl **完全相同**」——
     * 使用者无法借它去打 {@code http://127.0.0.1:其它端口}、{@code 169.254.169.254} 等其它内网地址。
     */
    private boolean exemptSelfOriginFromPrivateIpCheck = true;
}
