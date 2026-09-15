package cn.aiedge.dms.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 单量/时效趋势点（按日）
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "配送单量时效趋势点")
public class TrendPointVO {

    @Schema(description = "日期 yyyy-MM-dd")
    private String statDate;

    @Schema(description = "当日单量")
    private Long orderCount;

    @Schema(description = "当日完成量")
    private Long completedCount;

    @Schema(description = "当日准时量")
    private Long onTimeCount;

    @Schema(description = "当日准时率分母（有截止时间的已签收量）")
    private Long onTimeBase;

    @Schema(description = "当日准时率（%）")
    private BigDecimal onTimeRate;

    @Schema(description = "当日平均配送时长（分钟）")
    private BigDecimal avgMinutes;

    @Schema(description = "当日配送费")
    private BigDecimal deliveryFee;
}
