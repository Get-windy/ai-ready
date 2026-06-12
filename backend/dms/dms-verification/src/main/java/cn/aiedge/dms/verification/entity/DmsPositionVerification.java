package cn.aiedge.dms.verification.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 位置核验记录
 *
 * 系统定时比对配送员手机GPS位置与车辆GPS位置，
 * 记录每次核验结果，供异常检测使用。
 *
 * @author AI-Ready Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("dms_position_verification")
public class DmsPositionVerification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 绑定记录ID */
    private Long bindingId;

    /** 配送员ID */
    private Long riderId;

    /** 车辆ID */
    private Long vehicleId;

    /** 配送员上报位置 - 纬度 */
    private Double riderLat;

    /** 配送员上报位置 - 经度 */
    private Double riderLng;

    /** 配送员位置上报时间 */
    private LocalDateTime riderReportTime;

    /** 车辆GPS位置 - 纬度 */
    private Double vehicleLat;

    /** 车辆GPS位置 - 经度 */
    private Double vehicleLng;

    /** 车辆位置上报时间 */
    private LocalDateTime vehicleReportTime;

    /** 人车距离（米） */
    private Double distanceMeters;

    /** 偏差阈值（米） */
    private Double thresholdMeters;

    /** 是否超出阈值：0-正常 1-超阈值 */
    private Integer isAbnormal;

    /** 核验时间 */
    private LocalDateTime verifyTime;

    /** 核验结果说明 */
    private String verifyDesc;

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
