package cn.aiedge.dms.dispatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 派单策略与约束（《智能调度开发文档》§3.2 策略配置 Tab，落配置中心 `dms_config`）
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "派单策略与约束")
public class DispatchStrategyDTO {

    @Schema(description = "派单策略：NEAREST-最近可用 / BALANCED-负载均衡 / SCORE-评分优先 / AREA-区域分包")
    private String strategy;

    @Schema(description = "权重：距离")
    private BigDecimal weightDistance;

    @Schema(description = "权重：负载（在途单数）")
    private BigDecimal weightLoad;

    @Schema(description = "权重：评分")
    private BigDecimal weightScore;

    @Schema(description = "约束：单配送员最大并接在途单数（0=不限）")
    private Integer maxConcurrent;

    @Schema(description = "约束：是否仅向「空闲」配送员派单")
    private Boolean requireOnline;

    @Schema(description = "约束：单任务载重上限(kg，0=不限)")
    private BigDecimal maxLoadKg;

    @Schema(description = "约束：单任务容积上限(m³，0=不限)")
    private BigDecimal maxVolumeM3;

    @Schema(description = "超时升级阈值(分钟)")
    private Integer timeoutEscalateMinutes;

    @Schema(description = "区域分包严格模式（仅 AREA 策略生效）：true=任务线路未绑定该配送员即不可派；false=绑定者优先")
    private Boolean areaStrict;
}
