package cn.aiedge.dms.rider.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配送员信息
 */
@Data
@TableName("dms_rider")
public class DmsRider {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 关联系统用户ID */
    private Long userId;

    /** 类型：1-自有员工 2-众包兼职 3-外部平台骑手 4-社会车辆司机 */
    private Integer riderType;

    private String realName;
    private String phone;
    private String idCard;
    private String avatarUrl;

    /** 车辆类型 */
    private String vehicleType;

    /** 车牌号 */
    private String vehicleNo;

    /** 所属渠道ID */
    private Long channelId;

    /** 服务半径(公里) */
    private BigDecimal serviceRadius;

    /** 当前位置纬度 */
    private BigDecimal currentLat;

    /** 当前位置经度 */
    private BigDecimal currentLng;

    /** 最后位置上报时间 */
    private LocalDateTime lastReportTime;

    /** 状态：0-离线 1-空闲 2-忙碌 3-休息 */
    private Integer status;

    /** 审核状态：0-待审核 1-已通过 2-已拒绝 */
    private Integer verifyStatus;

    /** 综合评分（1.00-5.00） */
    private BigDecimal ratingScore;

    private Integer totalOrders;
    private Integer completedOrders;

    /** 保证金金额 */
    private BigDecimal depositAmount;

    /** 最大并行配送数 */
    private Integer maxConcurrent;

    /** 上班时间 HH:mm */
    private String workHoursStart;

    /** 下班时间 HH:mm */
    private String workHoursEnd;

    private String remark;

    // === Audit fields ===

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
