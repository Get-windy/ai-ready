package cn.aiedge.dms.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 配送仪表盘 KPI 聚合
 *
 * 口径固化（见《配送仪表盘开发文档》§3.2）：
 * · 单量/金额类指标：按 dms_task.create_time 落在查询区间统计；
 * · 状态分解：待分配=0、已分配/已接单=1,2、在途=3,4（取货中/配送中）、已完成=5,6（已签收/已完成）、
 *   已取消=7、异常=8、超时未签收=状态 0~4 且 deadline_time &lt; now()；
 * · 准时率 = 准时签收量 / 有截止时间的已签收量（completed_time &lt;= deadline_time）；
 * · 平均配送时长 = avg(completed_time - pickup_time)，单位分钟；
 * · 在线配送员 = last_report_time 在阈值（默认 2 分钟）内；
 * · 活跃配送员 = status ∈ (空闲, 忙碌)；活跃车辆 = status ∈ (使用中, 已出勤)；
 * · 待办计数（活跃绑定 / 待处理预警）为**实时快照**，不随查询区间与渠道筛选收敛。
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "配送仪表盘 KPI 聚合")
public class DashboardStatsVO {

    // ── 运力规模 ──
    @Schema(description = "配送员总数")
    private Long totalRiders;
    @Schema(description = "活跃配送员（空闲/忙碌）")
    private Long activeRiders;
    @Schema(description = "在线配送员（阈值内有位置上报）")
    private Long onlineRiders;

    @Schema(description = "车辆总数")
    private Long totalVehicles;
    @Schema(description = "活跃车辆（使用中/已出勤）")
    private Long activeVehicles;

    // ── 单量 ──
    @Schema(description = "区间内配送单量")
    private Long orderCount;
    @Schema(description = "待分配任务")
    private Long pendingOrders;
    @Schema(description = "已分配/已接单")
    private Long assignedOrders;
    @Schema(description = "在途任务（取货中/配送中）")
    private Long inTransitOrders;
    @Schema(description = "已完成（已签收/已完成）")
    private Long completedOrders;
    @Schema(description = "已取消")
    private Long cancelledOrders;
    @Schema(description = "异常")
    private Long exceptionOrders;
    @Schema(description = "超时未签收（已过截止时间且状态在待分配~配送中）")
    private Long overdueOrders;

    // ── 时效与金额 ──
    @Schema(description = "准时率（%）")
    private BigDecimal onTimeRate;
    @Schema(description = "平均配送时长（分钟）")
    private BigDecimal avgDeliveryMinutes;
    @Schema(description = "配送费合计")
    private BigDecimal deliveryFee;
    @Schema(description = "代收货款合计")
    private BigDecimal collectOnDelivery;
    @Schema(description = "货值合计")
    private BigDecimal goodsAmount;

    // ── 人车与预警 ──
    @Schema(description = "活跃人车绑定数")
    private Long activeBindingCount;
    @Schema(description = "待处理核验预警数")
    private Long pendingAlertCount;

    // ── 上下文（口径回显 + 前端轮询配置） ──
    @Schema(description = "时间范围标识")
    private String range;
    @Schema(description = "时间范围文案")
    private String rangeLabel;
    @Schema(description = "统计起始时间")
    private String startTime;
    @Schema(description = "统计截止时间")
    private String endTime;
    @Schema(description = "在线判定阈值（分钟）")
    private Integer onlineThresholdMinutes;
    @Schema(description = "自动刷新间隔（秒），取自配送参数 dashboard.refresh.seconds")
    private Integer refreshSeconds;
}
