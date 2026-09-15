package cn.aiedge.dms.tracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 轨迹里程聚合行（《配送跟踪开发文档》§3.4 `/mileage`：按配送员 / 任务 / 日聚合派生统计）
 *
 * <p>口径：相邻轨迹点按 Haversine 直线距离累加（不做地图路网纠偏），
 * 仅统计**同一分组内按时间排序**的相邻点，首点不计。</p>
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "轨迹里程聚合")
public class TrackingMileageVO {

    @Schema(description = "分组键：配送员ID / 任务ID / yyyy-MM-dd")
    private String groupKey;

    @Schema(description = "分组名称：配送员姓名 / 任务编号 / 日期")
    private String groupName;

    @Schema(description = "轨迹点数")
    private long pointCount;

    @Schema(description = "里程(米)")
    private BigDecimal mileageMeters;

    @Schema(description = "里程(公里，保留 2 位)")
    private BigDecimal mileageKm;

    @Schema(description = "平均速度(km/h)：按有速度的点求均值")
    private BigDecimal avgSpeed;

    @Schema(description = "首个轨迹点时间")
    private LocalDateTime firstTime;

    @Schema(description = "最后轨迹点时间")
    private LocalDateTime lastTime;
}
