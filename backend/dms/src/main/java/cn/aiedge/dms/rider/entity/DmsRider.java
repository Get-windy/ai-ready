package cn.aiedge.dms.rider.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 配送员信息
 *
 * <p>两类来源（见《配送员管理开发文档》§1）：企业员工（关联系统用户 userId + 部门 deptId）/
 * 外部平台配送员（关联渠道 channelId + 平台骑手ID platformRiderId）。</p>
 */
@Data
@TableName("dms_rider")
public class DmsRider {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 配送员编号（同租户唯一，PSY+4位序号） */
    private String riderNo;

    /** 关联系统用户ID（企业员工） */
    private Long userId;

    /** 类型：1-企业员工 2-众包兼职 3-外部平台配送员 4-社会车辆司机（RiderTypeEnum） */
    private Integer riderType;

    private String realName;
    private String phone;
    private String idCard;
    private String avatarUrl;

    /** 车辆类型 */
    private String vehicleType;

    /** 车牌号（自带车辆） */
    private String vehicleNo;

    /** 所属渠道ID（外部平台配送员） */
    private Long channelId;

    /** 外部平台骑手ID（外部平台配送员） */
    private String platformRiderId;

    /** 所属部门ID（企业员工） */
    private Long deptId;

    /** 所属部门名称快照 */
    private String deptName;

    /** 入职日期（企业员工） */
    private LocalDate entryDate;

    /** 资质/证照有效期（含外部平台渠道背书有效期） */
    private LocalDate qualificationExpireDate;

    /** 驾驶证号 */
    private String driverLicense;

    /** 健康证号 */
    private String healthCertNo;

    /** 结算方式：1-按单结算 2-月结 3-时段结算 */
    private Integer settleMethod;

    /** 准时率（百分比 0-100） */
    private BigDecimal punctualRate;

    /** 今日完成单量 */
    private Integer todayOrders;

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

    /** 审核备注（通过/拒绝原因） */
    private String verifyRemark;

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
