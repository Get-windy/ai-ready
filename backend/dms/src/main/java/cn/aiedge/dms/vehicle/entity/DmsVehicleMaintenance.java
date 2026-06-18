package cn.aiedge.dms.vehicle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 车辆维保记录
 *
 * @author AI-Ready Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("dms_vehicle_maintenance")
public class DmsVehicleMaintenance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 关联车辆ID */
    private Long vehicleId;

    /** 维保类型：1-保养 2-维修 3-年检 4-保险 5-事故 6-其他 */
    private Integer maintType;

    /** 维保单号 */
    private String maintNo;

    /** 维保日期 */
    private LocalDate maintDate;

    /** 维保内容描述 */
    private String maintContent;

    /** 维保费用 */
    private BigDecimal maintCost;

    /** 维保厂商 */
    private String maintVendor;

    /** 维保联系人 */
    private String maintContact;

    /** 维保联系电话 */
    private String maintPhone;

    /** 维保后里程(公里) */
    private Integer afterMaintMileage;

    /** 下次维保提醒里程(公里) */
    private Integer nextMaintMileage;

    /** 下次维保提醒日期 */
    private LocalDate nextMaintDate;

    /** 附件URLs(JSON数组) */
    private String attachmentUrls;

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
