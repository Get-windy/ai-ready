package cn.aiedge.dms.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 调度任务批量操作入参（《调度任务开发文档》§3.4 功能按钮：批量指派 / 批量取消）
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "调度任务批量操作入参")
public class DmsTaskBatchDTO {

    @Schema(description = "任务ID列表")
    private List<Long> taskIds;

    @Schema(description = "目标配送员ID（批量指派必填）")
    private Long riderId;

    @Schema(description = "操作原因（批量取消 / 改派留痕，可空）")
    private String reason;
}
