package cn.aiedge.dms.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 任务状态分布项
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "任务状态分布项")
public class TaskSummaryItemVO {

    @Schema(description = "状态码")
    private Integer status;

    @Schema(description = "状态名")
    private String statusName;

    @Schema(description = "数量")
    private Long count;

    @Schema(description = "占比（%）")
    private BigDecimal ratio;
}
