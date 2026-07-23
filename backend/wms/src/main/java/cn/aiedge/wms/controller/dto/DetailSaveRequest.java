package cn.aiedge.wms.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 作业单明细批量保存请求（先删后插）.
 * @param <T> 各作业明细实体类型
 */
@Data
@Schema(description = "作业单明细批量保存请求")
public class DetailSaveRequest<T> {

    @NotNull(message = "任务ID不能为空")
    @Schema(description = "作业任务ID（发货单为 shipId）")
    private Long taskId;

    @NotNull(message = "明细列表不能为空")
    @Schema(description = "明细列表（整体替换，先删后插）")
    private List<T> details;
}
