package cn.aiedge.dms.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 人车绑定创建 DTO（出车登记）
 */
@Data
@Schema(description = "人车绑定创建请求（出车登记）")
public class BindingCreateDTO {

    @NotNull(message = "配送员ID不能为空")
    @Schema(description = "配送员ID")
    private Long riderId;

    @NotNull(message = "车辆ID不能为空")
    @Schema(description = "车辆ID")
    private Long vehicleId;

    @Schema(description = "验车记录ID（必须先验车通过）")
    private Long inspectionId;

    @Schema(description = "绑定原因/任务描述")
    private String bindReason;
}
