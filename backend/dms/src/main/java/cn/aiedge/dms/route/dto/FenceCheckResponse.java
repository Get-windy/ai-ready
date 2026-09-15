package cn.aiedge.dms.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 电子围栏校验响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FenceCheckResponse {

    private boolean success;
    private String message;

    /** 单点校验结果 */
    private Boolean inside;

    /** 待校验点到围栏中心/边界的距离（米） */
    private Double distanceMeters;

    private Long fenceId;
    private String fenceName;
    /** CIRCLE / POLYGON / INLINE_CIRCLE / INLINE_POLYGON */
    private String fenceType;

    /** 批量校验结果 */
    private List<Item> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private double lat;
        private double lng;
        private String address;
        private Boolean inside;
        private Double distanceMeters;
    }
}
