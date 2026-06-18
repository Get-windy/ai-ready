package cn.aiedge.dms.vehicle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 自有配送车辆
 *
 * @author AI-Ready Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("dms_vehicle")
public class DmsVehicle {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 车辆编码 */
    private String vehicleCode;

    /** 车牌号 */
    private String plateNo;

    /** 车辆品牌 */
    private String brand;

    /** 车辆型号 */
    private String model;

    /** 车辆颜色 */
    private String color;

    /** 车辆类型：1-电动车 2-小货车 3-面包车 4-厢式货车 5-冷藏车 6-三轮车 */
    private Integer vehicleType;

    /** 车辆识别代号(VIN) */
    private String vin;

    /** 发动机号 */
    private String engineNo;

    /** 核定载质量(kg) */
    private BigDecimal ratedLoad;

    /** 核定载客人数 */
    private Integer ratedPassenger;

    /** 车辆自重(kg) */
    private BigDecimal curbWeight;

    /** 车辆尺寸 - 长(cm) */
    private BigDecimal lengthCm;

    /** 车辆尺寸 - 宽(cm) */
    private BigDecimal widthCm;

    /** 车辆尺寸 - 高(cm) */
    private BigDecimal heightCm;

    /** 车厢容积(m³) */
    private BigDecimal cargoVolume;

    /** 注册日期 */
    private LocalDate registerDate;

    /** 运营证号 */
    private String operatingPermitNo;

    /** 保险到期日 */
    private LocalDate insuranceExpireDate;

    /** 年检到期日 */
    private LocalDate inspectionExpireDate;

    /** 保养周期(公里) */
    private Integer maintenanceIntervalKm;

    /** 上次保养里程(公里) */
    private Integer lastMaintenanceKm;

    /** 上次保养日期 */
    private LocalDate lastMaintenanceDate;

    /** 当前里程(公里) */
    private Integer currentMileage;

    /** 车辆负责人ID（日常维护责任人，关联dms_rider/party）
     *  负责车辆日常保养、年检、保险等维护事务，与配送驾驶员可能不是同一人 */
    private Long vehicleManagerId;

    /** 车辆负责人姓名 */
    private String vehicleManagerName;

    /** 车辆负责人电话 */
    private String vehicleManagerPhone;

    /** 当前配送驾驶员ID（本次出车临时绑定的配送员，关联dms_rider）
     *  通过 dms_rider_vehicle_binding 管理绑定关系 */
    private Long currentRiderId;

    /** 当前配送驾驶员姓名 */
    private String currentRiderName;

    /** 车辆归属：1-公司自有 2-个人自带 3-租赁 */
    private Integer ownershipType;

    /** 所属部门 */
    private String department;

    /** 车辆状态：0-空闲 1-使用中 2-维修中 3-已报废 4-已出勤 */
    private Integer status;

    /** 是否启用GPS：0-否 1-是 */
    private Integer gpsEnabled;

    /** GPS设备编号 */
    private String gpsDeviceNo;

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
