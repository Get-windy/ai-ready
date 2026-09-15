package cn.aiedge.dms.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 配送员绩效 Top
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "配送员绩效排行")
public class TopRiderVO {

    @Schema(description = "配送员ID")
    private Long riderId;

    @Schema(description = "姓名")
    private String riderName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "综合评分")
    private BigDecimal ratingScore;

    @Schema(description = "区间接单量")
    private Long orderCount;

    @Schema(description = "区间完成量")
    private Long completedCount;

    @Schema(description = "准时量")
    private Long onTimeCount;

    @Schema(description = "准时率分母（有截止时间的完成量）")
    private Long onTimeBase;

    @Schema(description = "准时率（%）")
    private BigDecimal onTimeRate;

    @Schema(description = "平均配送时长（分钟）")
    private BigDecimal avgMinutes;
}
