package cn.aiedge.dms.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 逆地理编码响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReverseGeocodeResponse {

    private boolean success;
    private String message;

    /** 实际生效的地图服务商 */
    private String provider;

    /** 是否降级（未配置地图 Key，无法做真实逆地理编码） */
    private boolean degraded;

    /** 输入坐标（GCJ-02） */
    private Double lat;
    private Double lng;

    /** 格式化地址 */
    private String formattedAddress;

    /** 省 */
    private String province;

    /** 市 */
    private String city;

    /** 区 */
    private String district;

    /** 街道 */
    private String street;

    /** 门牌号 */
    private String streetNumber;

    /** 附近POI列表 */
    private List<PoiInfo> pois;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PoiInfo {
        private String name;
        private String type;
        private String address;
        private double lat;
        private double lng;
    }
}
