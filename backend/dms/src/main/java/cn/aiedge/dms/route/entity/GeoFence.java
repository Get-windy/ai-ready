package cn.aiedge.dms.route.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 电子围栏档案（表 dms_geo_fence）
 *
 * 几何模型：
 * · CIRCLE  —— centerLat / centerLng / radiusMeters
 * · POLYGON —— polygonPoints（"lng,lat;lng,lat;…"，至少 3 个顶点）
 *
 * 坐标统一 GCJ-02；判定由 GeoUtils 完成（不依赖地图服务 Key）。
 *
 * @author AI-Ready Team
 */
@Data
@TableName("dms_geo_fence")
public class GeoFence {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 围栏编码（同租户唯一） */
    private String fenceCode;

    /** 围栏名称 */
    private String fenceName;

    /** 围栏类型 CIRCLE / POLYGON */
    private String fenceType;

    /** 圆心纬度（圆形围栏；切换为多边形时需写空，故 updateStrategy=ALWAYS） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal centerLat;

    /** 圆心经度 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal centerLng;

    /** 半径（米） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal radiusMeters;

    /** 多边形顶点 "lng,lat;lng,lat;…" */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String polygonPoints;

    /** 绑定业务类型 ROUTE / CHANNEL / WAREHOUSE / OTHER */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String bizType;

    /** 绑定业务对象ID */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String bizId;

    /** 绑定业务对象名称快照 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String bizName;

    /** 状态 ENABLED / DISABLED */
    private String status;

    /** 备注（允许清空） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @Version
    private Integer version;
}
