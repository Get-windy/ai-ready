package cn.aiedge.dms.orderpool.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 发布任务到订单池（批量）
 *
 * <p>可选同时开启竞价：传 bidStartPrice + durationMinutes 即发布后立即进入竞价中。</p>
 */
@Data
@Schema(description = "发布到订单池请求")
public class PublishRequest {

    @NotEmpty(message = "请选择要发布的任务")
    @Schema(description = "配送任务ID列表")
    private List<Long> taskIds;

    @Schema(description = "配送费（不传则沿用任务上的配送费）")
    private BigDecimal deliveryFee;

    @Schema(description = "起拍价；与 durationMinutes 同时传入则发布后开启竞价")
    private BigDecimal bidStartPrice;

    @Schema(description = "竞价时长（分钟）")
    private Integer durationMinutes;
}
