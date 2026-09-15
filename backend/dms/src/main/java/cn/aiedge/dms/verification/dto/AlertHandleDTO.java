package cn.aiedge.dms.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 预警处理请求
 *
 * <p>处理人（handler）从登录态取，前端不再传，修复原 `@RequestParam handler` 必填导致的 400。</p>
 */
@Data
@Schema(description = "预警处理请求")
public class AlertHandleDTO {

    @NotNull(message = "处理状态不能为空")
    @Schema(description = "处理状态：1-已确认 2-已忽略 3-已处理")
    private Integer handleStatus;

    @Schema(description = "处理备注")
    private String remark;
}
