package cn.aiedge.dms.route.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 电子围栏档案（保存 / 详情 / 列表）
 */
@Data
public class GeoFenceDTO {

    private Long id;

    /** 围栏编码（新增时可空，由 next-code 生成） */
    private String fenceCode;

    private String fenceName;

    /** CIRCLE / POLYGON */
    private String fenceType;

    private BigDecimal centerLat;
    private BigDecimal centerLng;
    private BigDecimal radiusMeters;

    /** 多边形顶点 "lng,lat;lng,lat;…" */
    private String polygonPoints;

    private String bizType;
    private String bizId;
    private String bizName;

    /** ENABLED / DISABLED */
    private String status;

    private String remark;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
