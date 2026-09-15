package cn.aiedge.dms.tracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 实时跟踪统计卡（《实时跟踪开发文档》§3.2 统计卡片 / §3.5 `/tracking/stat`）
 *
 * <p>改造前由前端拉全量配送员 + 逐个最新位置在浏览器里算，现改为**后端一次聚合**。</p>
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "实时跟踪统计")
public class TrackingStatVO {

    @Schema(description = "配送员总数")
    private long riderTotal;

    @Schema(description = "在线配送员数（最近上报在阈值内）")
    private long onlineCount;

    @Schema(description = "离线配送员数")
    private long offlineCount;

    @Schema(description = "有位置上报记录的配送员数")
    private long withLocationCount;

    @Schema(description = "无任何位置数据的配送员数")
    private long noLocationCount;

    @Schema(description = "在途任务数（已分配~配送中）")
    private long activeTaskCount;

    @Schema(description = "超时在途任务数（已过要求完成时间仍未完成）")
    private long overdueTaskCount;

    @Schema(description = "今日轨迹点数")
    private long todayPointCount;

    @Schema(description = "今日异常预警数（超速/异常停留/超时在途，见 /tracking/alerts）")
    private long alertCount;

    @Schema(description = "在线判定阈值（分钟）")
    private Integer onlineMinutes;
}
