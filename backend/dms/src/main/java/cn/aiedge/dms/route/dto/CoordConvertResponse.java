package cn.aiedge.dms.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 坐标体系转换响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoordConvertResponse {

    private String from;
    private String to;

    /** 单点转换结果 */
    private Double lat;
    private Double lng;

    /** 批量转换结果 */
    private List<Item> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private double lat;
        private double lng;
        private String address;
    }
}
