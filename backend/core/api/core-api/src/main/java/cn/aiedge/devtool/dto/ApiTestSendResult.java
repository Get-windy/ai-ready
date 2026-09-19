package cn.aiedge.devtool.dto;

import java.util.Map;

/**
 * 一次受控出站调用的结果（服务层 → 控制器）。
 *
 * <p>{@code success=false} 表示**请求根本没有发出去**（被闸门拒绝 / 参数非法 / 超时 / 连接失败），
 * 此时 {@code rejectReason} 必非空且是可直接展示的中文原因 —— 前端据此如实展示，不做假数据兜底。
 * {@code success=true} 表示请求已发出并已拿到响应（**哪怕响应是 4xx/5xx**：那是目标服务端的真实结论，
 * 不是本端点的失败）。
 *
 * @param success        请求是否成功发出并读回响应
 * @param message        人类可读结论（前端 resultNotice 直接展示）
 * @param rejectReason   被拒绝/失败的具体原因（success=false 时非空）
 * @param targetUrl      实际请求的地址（同源相对路径会展开为绝对地址，便于使用者对照）
 * @param status         HTTP 状态码（success=false 时为 null）
 * @param elapsedMs      总耗时（连接 + 读取响应体）
 * @param responseBytes  实际读回并保留的响应体字节数
 * @param truncated      响应体是否因超过上限被截断
 * @param redirectBlocked 目标是否返回了 3xx（已按安全策略阻止跟随）
 * @param responseHeaders 脱敏后的响应头
 * @param responseBody   截断后的响应体（UTF-8 容错解码）
 * @since 2026-09-19
 */
public record ApiTestSendResult(boolean success,
                                String message,
                                String rejectReason,
                                String targetUrl,
                                Integer status,
                                Long elapsedMs,
                                Integer responseBytes,
                                boolean truncated,
                                boolean redirectBlocked,
                                Map<String, String> responseHeaders,
                                String responseBody) {

    public static ApiTestSendResult rejected(String reason, String targetUrl) {
        return new ApiTestSendResult(false, reason, reason, targetUrl, null, null, null, false, false, Map.of(), null);
    }

    public static ApiTestSendResult failed(String reason, String targetUrl, Long elapsedMs) {
        return new ApiTestSendResult(false, reason, reason, targetUrl, null, elapsedMs, null, false, false, Map.of(), null);
    }
}
