package cn.aiedge.dms.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 地理编码响应
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class GeocodeResponse {

    private boolean success;
    private String message;

    /** 实际生效的地图服务商 */
    private String provider;

    /** 是否降级（未配置地图 Key，无法做真实地理编码） */
    private boolean degraded;

    /** 是否命中缓存 */
    private boolean cached;

    /** 纬度（candidates[0]，便于单结果调用方直接使用） */
    private Double lat;

    /** 经度（candidates[0]） */
    private Double lng;

    /** 格式化地址（candidates[0]） */
    private String formattedAddress;

    /** 省 */
    private String province;

    /** 市 */
    private String city;

    /** 区 */
    private String district;

    /** 模糊匹配候选列表（金标准：一次查询返回多个候选供选择） */
    private List<GeocodeCandidate> candidates;

    /**
     * 地理编码候选
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeocodeCandidate {
        private String formattedAddress;
        private Double lat;
        private Double lng;
        private String province;
        private String city;
        private String district;
        /** 行政区划编码（高德 adcode） */
        private String adcode;
        /** 匹配级别（高德 level，如 门牌号/兴趣点/区县） */
        private String level;
    }
}
