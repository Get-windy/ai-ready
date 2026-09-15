package cn.aiedge.dms.tracking.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 位置追踪记录
 *
 * 记录配送员的实时位置信息，包括GPS坐标、速度、方向等。
 * 数据来源包括APP端上报和后台查询。
 *
 * @author AI-Ready Team
 */
@Data
@TableName("dms_tracking")
public class DmsTracking {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 配送员ID */
    private Long riderId;

    /** 关联任务ID */
    private Long taskId;

    /** 纬度 */
    private BigDecimal lat;

    /** 经度 */
    private BigDecimal lng;

    /** 速度（km/h） */
    private BigDecimal speed;

    /** 方向角度（0-360，正北为0顺时针） */
    private BigDecimal direction;

    /** 定位精度（米）：越大越不可信，用于轨迹可信度与对账 */
    private BigDecimal accuracy;

    /** 上报位置地址（上报端带回；服务端逆地理编码待接《路线规划》） */
    private String address;

    /** 上报时间 */
    private LocalDateTime reportTime;

    /** 数据来源：1-APP上报 2-后台补录 3-渠道回传（TrackingSourceEnum） */
    private Integer source;

    /** 逻辑删除 */
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
