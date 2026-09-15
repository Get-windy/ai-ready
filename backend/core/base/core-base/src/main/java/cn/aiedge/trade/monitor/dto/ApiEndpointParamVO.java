package cn.aiedge.trade.monitor.dto;

/**
 * 开放接口参数定义（驱动「快速联调」参数表单）
 *
 * @param name        参数名
 * @param label       中文名
 * @param in          位置: path / query / body
 * @param type        类型: string / number / json
 * @param required    是否必填
 * @param sample      示例值（页面默认填充，便于一键发送）
 * @param description 说明
 */
public record ApiEndpointParamVO(
        String name,
        String label,
        String in,
        String type,
        boolean required,
        String sample,
        String description) {

    public static ApiEndpointParamVO path(String name, String label, boolean required, String sample, String description) {
        return new ApiEndpointParamVO(name, label, "path", "string", required, sample, description);
    }

    public static ApiEndpointParamVO query(String name, String label, boolean required, String sample, String description) {
        return new ApiEndpointParamVO(name, label, "query", "string", required, sample, description);
    }

    public static ApiEndpointParamVO queryNumber(String name, String label, boolean required, String sample, String description) {
        return new ApiEndpointParamVO(name, label, "query", "number", required, sample, description);
    }

    public static ApiEndpointParamVO body(String name, String label, boolean required, String sample, String description) {
        return new ApiEndpointParamVO(name, label, "body", "json", required, sample, description);
    }
}
