package cn.aiedge.trade.monitor.dto;

import java.util.Map;

/**
 * 「快速联调」发送请求
 *
 * @param apiKey 接口键（取自 {@link cn.aiedge.trade.monitor.OpenApiCatalog}，服务端据此解析路径与参数位置）
 * @param params 参数值（key = 参数名；body 类参数传**原始 JSON 字符串**）
 */
public record SandboxInvokeRequest(String apiKey, Map<String, String> params) {
}
