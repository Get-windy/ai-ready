package cn.aiedge.dms.verification.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 出车验车记录
 *
 * 配送员在每次出车前必须对车辆进行安全检查，记录车辆状况。
 * 验车通过后方可进行人车绑定和出车。
 *
 * @author AI-Ready Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("dms_vehicle_inspection")
public class DmsVehicleInspection {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 车辆ID */
    private Long vehicleId;

    /** 车牌号 */
    private String plateNo;

    /** 配送员ID */
    private Long riderId;

    /** 配送员姓名 */
    private String riderName;

    /** 验车类型：1-出车验车 2-收车验车 3-定期检查 */
    private Integer inspectionType;

    /** 验车时间 */
    private LocalDateTime inspectionTime;

    /** 车辆外观状况：0-正常 1-异常 */
    private Integer exteriorStatus;

    /** 外观异常描述 */
    private String exteriorRemark;

    /** 外观照片URLs(JSON数组) */
    private String exteriorPhotos;

    /** 轮胎状况：0-正常 1-异常 */
    private Integer tireStatus;

    /** 轮胎异常描述 */
    private String tireRemark;

    /** 灯光状况：0-正常 1-异常 */
    private Integer lightStatus;

    /** 灯光异常描述 */
    private String lightRemark;

    /** 刹车状况：0-正常 1-异常 */
    private Integer brakeStatus;

    /** 刹车异常描述 */
    private String brakeRemark;

    /** 车内清洁状况：0-正常 1-异常 */
    private Integer cleanlinessStatus;

    /** 清洁异常描述 */
    private String cleanlinessRemark;

    /** 里程数（验车时） */
    private Integer mileage;

    /** 油量/电量百分比 */
    private Integer fuelLevel;

    /** 灭火器：0-正常 1-缺失/过期 */
    private Integer fireExtinguisher;

    /** 三角警示牌：0-有 1-缺失 */
    private Integer warningTriangle;

    /** 验车结果：0-未检查 1-通过 2-不通过（禁止出车） */
    private Integer result;

    /** 审核人（车管员确认） */
    private String reviewer;

    /** 审核时间 */
    private LocalDateTime reviewTime;

    /** 审核意见 */
    private String reviewRemark;

    /** 关联绑定记录ID（验车通过后绑定） */
    private Long bindingId;

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
