package cn.aiedge.base.social;

import cn.aiedge.common.exception.BusinessException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 三方 OAuth 适配器共用的 HTTP + JSON 小工具。
 * <p>复用项目既有的 hutool，不新增任何依赖。</p>
 *
 * @author AI-Ready Team
 * @since 0.3.28
 */
@Slf4j
final class SocialHttp {

    /** 三方接口普遍不快，但也不该长时间占着请求线程 */
    private static final int TIMEOUT_MILLIS = 8000;

    private SocialHttp() {
    }

    static JSONObject get(String url, Map<String, String> headers) {
        HttpRequest request = HttpRequest.get(url).timeout(TIMEOUT_MILLIS);
        if (headers != null) {
            headers.forEach(request::header);
        }
        return parse(request.execute(), url);
    }

    static JSONObject postJson(String url, Map<String, String> headers, Object body) {
        HttpRequest request = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .timeout(TIMEOUT_MILLIS);
        if (headers != null) {
            headers.forEach(request::header);
        }
        return parse(request.body(JSONUtil.toJsonStr(body)).execute(), url);
    }

    private static JSONObject parse(HttpResponse response, String url) {
        String body = response.body();
        if (!response.isOk()) {
            log.warn("[三方登录] 调用失败 url={} http={} body={}", url, response.getStatus(), body);
            throw BusinessException.badRequest("三方平台调用失败（HTTP " + response.getStatus() + "）");
        }
        if (body == null || body.isBlank() || !JSONUtil.isTypeJSONObject(body)) {
            log.warn("[三方登录] 返回非 JSON url={} body={}", url, body);
            throw BusinessException.badRequest("三方平台返回格式异常");
        }
        return JSONUtil.parseObj(body);
    }

    /**
     * 取字符串；缺失或为字面量 {@code "null"} 时返回 null。
     * <p>部分平台（如飞书的部分字段）会把空值序列化成字符串 "null"，故一并归一。</p>
     */
    static String str(JSONObject json, String key) {
        Object value = json.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return "null".equals(text) ? null : text;
    }
}
