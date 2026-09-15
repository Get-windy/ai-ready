package cn.aiedge.dms.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 地图服务连通性自检结果
 *
 * 用当前生效的 Key 真实调用一次「地址编码」样本（不走缓存），据此判断 Key 是否可用，
 * 避免"配了 Key 但无效/未开通服务"长期潜伏到业务侧才发现。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteVerifyResponse {

    /** 自检是否通过（Key 有效且地图服务返回坐标） */
    private boolean ok;

    /** 实际生效的服务商 */
    private String provider;

    /** Key 来源：ENV / SPRING / CONFIG / NONE */
    private String keySource;

    /** 该服务商对应的环境变量名 */
    private String envVarName;

    /** 该服务商在《配送参数》中的配置键 */
    private String configKey;

    /** 是否为降级模式（本地直线，无 Key） */
    private boolean degraded;

    /** 调用耗时（毫秒） */
    private Long latencyMs;

    /** 自检样本地址 */
    private String sampleAddress;

    /** 样例返回坐标（有则回填） */
    private Double lat;

    private Double lng;

    private String formattedAddress;

    /** 结论说明（失败时给出原因与处置建议） */
    private String message;
}
