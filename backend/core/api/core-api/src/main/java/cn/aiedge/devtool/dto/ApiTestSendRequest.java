package cn.aiedge.devtool.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API 测试台「发送请求」入参。
 *
 * <p><b>只承载使用者显式填写的内容</b>：目标地址、方法、请求头、请求体。
 * 本端点**不会**从入站的 HTTP 请求里复制任何头（那才是平台凭据所在的地方），
 * 因此这里没有 HttpServletRequest —— 没有「顺手转发一下原请求头」的可能。
 *
 * <p><b>为什么 headers / body 标了 {@code WRITE_ONLY}</b>：本方法同时被
 * {@code @OperationLog} 切面用 ObjectMapper 序列化后写入 {@code sys_oper_log.request_params}。
 * 审计只需要「谁、何时、打了哪个 URL」，**不应该**把使用者的请求体（可能含业务敏感数据）
 * 与请求头（可能含使用者自行粘贴的凭据）落进审计表 —— 故这两个字段只入不出。
 *
 * @since 2026-09-19
 */
@Data
public class ApiTestSendRequest {

    /**
     * 目标地址。两种形态：
     * <ul>
     *   <li>同源相对路径（以 {@code /api/} 开头）—— 拼接服务端配置的 {@code app.api-test.self-base-url}；</li>
     *   <li>完整 http(s) URL —— 只有与 self-base-url 同 origin 时才算同源，其它一律走服务端白名单。</li>
     * </ul>
     */
    @Schema(description = "目标地址：/api/ 开头的同源相对路径，或完整的 http(s) URL", example = "/api/monitor/health")
    private String url;

    /** 请求方法。白名单：GET / HEAD / POST / PUT / PATCH / DELETE */
    @Schema(description = "请求方法", example = "GET")
    private String method;

    /** 使用者显式填写的请求头（**只有这里的内容才会被转发**，且仍要过逐跳头/内部头黑名单） */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(description = "自定义请求头（仅这些头会被转发；逐跳头与平台内部头会被拒绝）")
    private Map<String, String> headers = new LinkedHashMap<>();

    /** 请求体原文（GET/HEAD 不允许携带；上限由 app.api-test.max-request-bytes 控制） */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(description = "请求体（GET/HEAD 不携带）")
    private String body;
}
