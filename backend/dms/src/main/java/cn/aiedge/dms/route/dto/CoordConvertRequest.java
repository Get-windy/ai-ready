package cn.aiedge.dms.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 坐标体系转换请求
 *
 * 支持单点与批量（points 优先）；from/to 取值 WGS84 / GCJ02 / BD09。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoordConvertRequest {

    private Double lat;
    private Double lng;

    private List<FenceCheckRequest.Point> points;

    /** 源坐标体系，默认 WGS84 */
    private String from;

    /** 目标坐标体系，默认 GCJ02 */
    private String to;
}
