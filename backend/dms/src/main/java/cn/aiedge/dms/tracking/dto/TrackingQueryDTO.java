package cn.aiedge.dms.tracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 轨迹台账查询条件（《配送跟踪开发文档》§3.3）
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "轨迹台账查询条件")
public class TrackingQueryDTO {

    private Integer pageNum;

    private Integer pageSize;

    @Schema(description = "配送员ID（选择器）")
    private Long riderId;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "任务编号（模糊，反查任务ID集合）")
    private String taskNo;

    @Schema(description = "配送员姓名/手机号（模糊，反查配送员ID集合）")
    private String riderKeyword;

    @Schema(description = "配送员状态（左面板过滤）：0-离线 1-空闲 2-忙碌 3-休息")
    private Integer riderStatus;

    @Schema(description = "仅今日有上报的配送员（左面板过滤）")
    private Boolean activeToday;

    @Schema(description = "在线过滤（心跳派生，左面板）：true-在线（最近上报在阈值内）false-离线 null-不限")
    private Boolean online;

    @Schema(description = "开始时间（yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd）")
    private String startTime;

    @Schema(description = "结束时间（yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd）")
    private String endTime;

    @Schema(description = "来源（多选 CSV 或重复参数）：1-APP 2-后台 3-渠道")
    private List<Integer> sources;

    @Schema(description = "速度下限(km/h)：用于筛异常点（超速）")
    private BigDecimal minSpeed;

    @Schema(description = "仅显示有任务关联的点")
    private Boolean onlyWithTask;

    @Schema(description = "排序字段：reportTime / speed")
    private String sortField;

    @Schema(description = "排序方向：asc / desc（缺省 desc）")
    private String sortOrder;
}
