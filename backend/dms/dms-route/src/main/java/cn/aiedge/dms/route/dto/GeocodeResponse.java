package cn.aiedge.dms.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 地理编码响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeocodeResponse {

    private boolean success;
    private String message;

    /** 纬度 */
    private Double lat;

    /** 经度 */
    private Double lng;

    /** 格式化地址 */
    private String formattedAddress;

    /** 省 */
    private String province;

    /** 市 */
    private String city;

    /** 区 */
    private String district;
}
