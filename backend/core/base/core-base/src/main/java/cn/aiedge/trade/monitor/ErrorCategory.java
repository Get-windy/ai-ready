package cn.aiedge.trade.monitor;

import java.util.Locale;

/**
 * 失败原因分类（`inventory_sync_record.error_category` / 告警建议）
 *
 * <p>《API监控开发文档》§3.5.6：失败原因归类（网络 / 鉴权 / 参数 / 渠道限流 / 业务拒绝）。
 * 分类由**错误信息关键字**推导，未命中归 {@link #UNKNOWN}，不臆造。</p>
 */
public enum ErrorCategory {

    /** 网络/超时/连接类 */
    NETWORK("网络异常"),

    /** 鉴权/令牌/签名类 */
    AUTH("鉴权失败"),

    /** 参数/格式/必填类 */
    PARAM("参数错误"),

    /** 渠道限流/频控类 */
    RATE_LIMIT("渠道限流"),

    /** 业务拒绝（库存不足/商品不可售等） */
    BIZ_REJECT("业务拒绝"),

    /** 未归类 */
    UNKNOWN("未知原因");

    private final String label;

    ErrorCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 按错误信息推导分类（大小写无关；关键字命中顺序 = 从具体到宽泛）
     *
     * @param errorMsg 错误信息（可为空）
     * @return 分类，永不为 null
     */
    public static ErrorCategory classify(String errorMsg) {
        if (errorMsg == null || errorMsg.isBlank()) {
            return UNKNOWN;
        }
        String msg = errorMsg.toLowerCase(Locale.ROOT);
        if (containsAny(msg, "timeout", "timed out", "connect", "connection", "network", "unreachable", "超时", "网络", "连接")) {
            return NETWORK;
        }
        if (containsAny(msg, "401", "403", "unauthorized", "forbidden", "token", "sign", "signature", "鉴权", "签名", "令牌", "授权")) {
            return AUTH;
        }
        if (containsAny(msg, "429", "rate limit", "ratelimit", "too many requests", "限流", "频控")) {
            return RATE_LIMIT;
        }
        if (containsAny(msg, "param", "invalid", "missing", "required", "format", "参数", "必填", "格式", "不能为空")) {
            return PARAM;
        }
        if (containsAny(msg, "不足", "不可售", "已下架", "拒绝", "库存", "reject", "refuse", "not available")) {
            return BIZ_REJECT;
        }
        return UNKNOWN;
    }

    private static boolean containsAny(String msg, String... keys) {
        for (String key : keys) {
            if (msg.contains(key)) {
                return true;
            }
        }
        return false;
    }
}
