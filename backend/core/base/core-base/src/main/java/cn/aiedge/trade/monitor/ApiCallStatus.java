package cn.aiedge.trade.monitor;

/**
 * 接口调用状态（`api_access_log.status`）
 *
 * <p>口径：HTTP/业务层未抛异常且响应码 &lt; 400 → {@link #SUCCESS}，否则 {@link #FAIL}。
 * 与《API监控开发文档》§3.2「失败次数 = count(status = FAIL)」一致。</p>
 */
public enum ApiCallStatus {
    SUCCESS,
    FAIL
}
