package cn.aiedge.trade.monitor.dto;

/**
 * 「快速联调」结果（真实 HTTP 自检，非模拟）
 *
 * <p>沙箱通过 **RestTemplate 回环调用**本实例的 `/api/open/**`（携带联调人的登录态），
 * 因此返回的是**真实**响应体、真实耗时与脱敏后的请求地址；每次调用都写一条
 * `direction = SANDBOX` 的调用日志，即页面「联调历史」。</p>
 *
 * @param requestId    请求号（同时写入调用日志，可对账）
 * @param apiKey       接口键
 * @param apiName      接口中文名
 * @param method       HTTP 方法
 * @param url          实际请求地址（query 脱敏）
 * @param httpStatus   HTTP 状态码（回环失败为 0）
 * @param success      是否成功（HTTP &lt; 400 且业务码为 200）
 * @param bizCode      业务响应码（Result.code，解析不到为 null）
 * @param costMs       耗时(ms)
 * @param responseBody 响应体（截断，敏感值脱敏）
 * @param errorMsg     失败原因
 * @param invokeTime   调用时间
 */
public record SandboxResultVO(
        String requestId,
        String apiKey,
        String apiName,
        String method,
        String url,
        int httpStatus,
        boolean success,
        Integer bizCode,
        int costMs,
        String responseBody,
        String errorMsg,
        String invokeTime) {
}
