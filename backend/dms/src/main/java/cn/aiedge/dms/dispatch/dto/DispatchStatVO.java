package cn.aiedge.dms.dispatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 调度效果复盘（《智能调度开发文档》§3.3 `/dispatch/stat`）
 *
 * <p>口径：自动 vs 手工占比按 `dms_task.dispatch_type`（1-自动 2-手动 3-抢单 4-竞价）；
 * 平均派单耗时 = avg(dispatchTime - createTime)（已指派任务）；超时 = 在途任务已过 deadline。</p>
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "调度效果统计")
public class DispatchStatVO {

    @Schema(description = "任务总数（全部状态）")
    private long taskTotal;

    @Schema(description = "待分配任务数")
    private long pendingCount;

    @Schema(description = "已指派任务数（dispatch_time 非空）")
    private long assignedCount;

    @Schema(description = "自动调度数")
    private long autoCount;

    @Schema(description = "手工指派数")
    private long manualCount;

    @Schema(description = "抢单数（归口《订单池》，本页只统计占比）")
    private long grabCount;

    @Schema(description = "竞价数（归口《订单池》，本页只统计占比）")
    private long bidCount;

    @Schema(description = "其他方式（未记方式的历史数据）")
    private long otherCount;

    @Schema(description = "自动调度占比(%)")
    private BigDecimal autoRate;

    @Schema(description = "平均派单耗时(秒)：任务创建 → 指派")
    private BigDecimal avgDispatchSeconds;

    @Schema(description = "在途任务数（已分配~配送中）")
    private long activeCount;

    @Schema(description = "超时在途数（已过要求完成时间）")
    private long overdueCount;

    @Schema(description = "超时率(%)：超时在途 / 在途")
    private BigDecimal overdueRate;

    @Schema(description = "未指派任务数（待分配中，含扫描失败）")
    private long unassignedCount;
}
