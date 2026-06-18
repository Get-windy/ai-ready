package cn.aiedge.dms.verification.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 配送员-车辆绑定记录
 *
 * 每次出车前绑定人车关系，配送完成后交车解绑。
 * 绑定期间系统持续进行位置核验。
 *
 * @author AI-Ready Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("dms_rider_vehicle_binding")
public class DmsRiderVehicleBinding {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 配送员ID（关联 dms_rider） */
    private Long riderId;

    /** 配送员姓名 */
    private String riderName;

    /** 配送员手机号 */
    private String riderPhone;

    /** 车辆ID（关联 dms_vehicle） */
    private Long vehicleId;

    /** 车牌号 */
    private String plateNo;

    /** 绑定时间（出车） */
    private LocalDateTime bindTime;

    /** 绑定时的车辆里程 */
    private Integer bindMileage;

    /** 绑定时的车辆位置 - 纬度 */
    private Double bindLat;

    /** 绑定时的车辆位置 - 经度 */
    private Double bindLng;

    /** 交车时间 */
    private LocalDateTime handoverTime;

    /** 交车时的车辆里程 */
    private Integer handoverMileage;

    /** 交车时的车辆位置 - 纬度 */
    private Double handoverLat;

    /** 交车时的车辆位置 - 经度 */
    private Double handoverLng;

    /** 绑定原因/任务描述 */
    private String bindReason;

    /** 状态：0-绑定中 1-已交车 2-异常解绑 */
    private Integer status;

    /** 备注 */
    private String remark;

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
