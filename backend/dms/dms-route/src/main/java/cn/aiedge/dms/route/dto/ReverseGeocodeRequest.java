package cn.aiedge.dms.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 逆地理编码请求（坐标 → 地址）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReverseGeocodeRequest {

    private double lat;
    private double lng;

    /** 返回附近POI数量（0-不返回） */
    private Integer poiRadius;
}
