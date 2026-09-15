package cn.aiedge.trade.monitor;

/**
 * 接口调用方向（`api_access_log.direction`）
 *
 * <p>三类调用共用一张日志表：入站、出站、联调；页面「接口调用日志」按方向标签区分，
 * 「快速联调」的历史记录即 {@link #SANDBOX} 方向的分页。</p>
 */
public enum ApiCallDirection {

    /** 外部系统调用我方开放接口（`/api/open/**`） */
    IN,

    /** 我方调用外部渠道/平台 */
    OUT,

    /** 页面「快速联调」自检 */
    SANDBOX;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (ApiCallDirection direction : values()) {
            if (direction.name().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
