package cn.aiedge.dms.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 巡检审核请求
 *
 * <p>前端历史字段为 {@code reviewResult}，后端为 {@code result} → 400；
 * 本 DTO 同时接受两个字段名（reviewResult 优先），审核人从登录态取。</p>
 */
@Data
@Schema(description = "巡检审核请求")
public class InspectionReviewDTO {

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "审核结果：1-通过 0-不通过")
    private Integer result;

    @Schema(description = "兼容字段：审核结果（1-通过 0-不通过），传入时覆盖 result")
    private Integer reviewResult;

    @Schema(description = "审核意见")
    private String remark;

    /** 归一化：优先取 reviewResult */
    public Integer resolveResult() {
        return reviewResult != null ? reviewResult : result;
    }
}
