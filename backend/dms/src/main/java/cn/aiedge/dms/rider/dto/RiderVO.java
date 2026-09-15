package cn.aiedge.dms.rider.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 配送员列表/详情视图（含归属渠道·部门·系统账号、在线心跳、资质到期等派生字段）
 */
@Data
@Schema(description = "配送员信息")
public class RiderVO {

    private Long id;

    @Schema(description = "配送员编号")
    private String riderNo;

    @Schema(description = "姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "类型：1-企业员工 2-众包兼职 3-外部平台配送员 4-社会车辆司机")
    private Integer riderType;

    @Schema(description = "类型文案")
    private String riderTypeText;

    @Schema(description = "状态：0-离线 1-空闲 2-忙碌 3-休息")
    private Integer status;

    @Schema(description = "状态文案")
    private String statusText;

    @Schema(description = "审核状态：0-待审核 1-已通过 2-已拒绝")
    private Integer verifyStatus;

    @Schema(description = "审核状态文案")
    private String verifyStatusText;

    @Schema(description = "审核备注")
    private String verifyRemark;

    @Schema(description = "身份证号（脱敏）")
    private String idCard;

    @Schema(description = "关联系统用户ID（企业员工）")
    private Long userId;

    @Schema(description = "关联系统账号（企业员工）")
    private String userName;

    @Schema(description = "所属部门ID")
    private Long deptId;

    @Schema(description = "所属部门")
    private String deptName;

    @Schema(description = "入职日期")
    private LocalDate entryDate;

    @Schema(description = "归属渠道ID（外部平台配送员）")
    private Long channelId;

    @Schema(description = "归属渠道名称")
    private String channelName;

    @Schema(description = "外部平台骑手ID")
    private String platformRiderId;

    @Schema(description = "资质/证照有效期")
    private LocalDate qualificationExpireDate;

    @Schema(description = "资质是否已过期")
    private Boolean qualificationExpired;

    @Schema(description = "资质剩余天数")
    private Long qualificationRemainDays;

    @Schema(description = "驾驶证号")
    private String driverLicense;

    @Schema(description = "健康证号")
    private String healthCertNo;

    @Schema(description = "结算方式：1-按单结算 2-月结 3-时段结算")
    private Integer settleMethod;

    @Schema(description = "结算方式文案")
    private String settleMethodText;

    @Schema(description = "评分（1-5）")
    private BigDecimal ratingScore;

    @Schema(description = "累计单量")
    private Integer totalOrders;

    @Schema(description = "今日单量")
    private Integer todayOrders;

    @Schema(description = "准时率（%）")
    private BigDecimal punctualRate;

    @Schema(description = "车辆类型")
    private String vehicleType;

    @Schema(description = "自带车牌号")
    private String vehicleNo;

    @Schema(description = "当前绑定车辆车牌")
    private String vehiclePlate;

    @Schema(description = "在线状态（心跳派生）：1-在线 0-离线")
    private Integer onlineStatus;

    @Schema(description = "在线状态文案")
    private String onlineStatusText;

    @Schema(description = "最近位置上报时间")
    private LocalDateTime lastReportTime;

    @Schema(description = "当前纬度")
    private BigDecimal currentLat;

    @Schema(description = "当前经度")
    private BigDecimal currentLng;

    @Schema(description = "服务半径（公里）")
    private BigDecimal serviceRadius;

    @Schema(description = "最大并行配送数")
    private Integer maxConcurrent;

    @Schema(description = "上班时间")
    private String workHoursStart;

    @Schema(description = "下班时间")
    private String workHoursEnd;

    @Schema(description = "保证金金额")
    private BigDecimal depositAmount;

    @Schema(description = "备注")
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
