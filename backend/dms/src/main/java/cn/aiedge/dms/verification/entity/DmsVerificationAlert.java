package cn.aiedge.dms.verification.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 核验异常告警记录
 *
 * 当人车位置核验连续多次异常、或检测到异常滞留/偏离路线时产生告警。
 *
 * @author AI-Ready Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("dms_verification_alert")
public class DmsVerificationAlert {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 绑定记录ID */
    private Long bindingId;

    /** 告警类型：1-人车位置分离 2-异常滞留 3-速度异常 4-偏离路线 5-绑定超时 6-非工作时段用车 */
    private Integer alertType;

    /** 告警级别：1-提示 2-警告 3-严重 */
    private Integer alertLevel;

    /** 配送员ID */
    private Long riderId;

    /** 配送员姓名 */
    private String riderName;

    /** 车辆ID */
    private Long vehicleId;

    /** 车牌号 */
    private String plateNo;

    /** 涉及任务ID */
    private Long taskId;

    /** 告警时的配送员位置 - 纬度 */
    private Double riderLat;

    /** 告警时的配送员位置 - 经度 */
    private Double riderLng;

    /** 告警时的车辆位置 - 纬度 */
    private Double vehicleLat;

    /** 告警时的车辆位置 - 经度 */
    private Double vehicleLng;

    /** 人车距离（米） */
    private Double distanceMeters;

    /** 滞留时长（秒，滞留告警时使用） */
    private Long stayDuration;

    /** 告警内容描述 */
    private String alertContent;

    /** 处理状态：0-未处理 1-已确认 2-已忽略 3-已处理 */
    private Integer handleStatus;

    /** 处理人 */
    private String handler;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 处理备注 */
    private String handleRemark;

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
