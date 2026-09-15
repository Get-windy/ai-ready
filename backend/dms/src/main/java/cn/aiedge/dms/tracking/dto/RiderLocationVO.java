package cn.aiedge.dms.tracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配送员位置聚合行（《实时跟踪开发文档》§3.2「配送员最新位置」/ §3.5 `/tracking/rider-page`）
 *
 * <p>替代改造前的 **N+1**（先拉 100 个配送员再逐个 `/latest`）：一次分页返回
 * 配送员 + 最新位置 + 在线态 + 今日里程/单量 + 在途负载。</p>
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "配送员实时位置")
public class RiderLocationVO {

    private Long riderId;

    private String riderNo;

    private String riderName;

    private String riderPhone;

    private String riderTypeText;

    /** 配送员档案状态：0-离线 1-空闲 2-忙碌 3-休息 */
    private Integer status;

    private String statusText;

    @Schema(description = "是否在线（最近上报时间在阈值内，阈值见 dms.tracking.online.minutes）")
    private Boolean online;

    @Schema(description = "在线判定阈值（分钟）")
    private Integer onlineMinutes;

    private BigDecimal lat;

    private BigDecimal lng;

    private BigDecimal speed;

    private BigDecimal direction;

    private String directionText;

    private BigDecimal accuracy;

    private String address;

    private LocalDateTime lastReportTime;

    @Schema(description = "最近上报距今秒数")
    private Long lastReportAgoSeconds;

    /** 今日里程(km)：今日轨迹点 Haversine 累计 */
    private BigDecimal todayMileageKm;

    /** 今日轨迹点数 */
    private Integer todayPointCount;

    /** 今日完成任务数 */
    private Integer todayDoneTasks;

    /** 在途负载（未完成任务数） */
    private Integer activeTasks;

    /** 当前车辆（车牌号，取自最近一次带 task 的上报或任务快照） */
    private String vehicleName;
}
