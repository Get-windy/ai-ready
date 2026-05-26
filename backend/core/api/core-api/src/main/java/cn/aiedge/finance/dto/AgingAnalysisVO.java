package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 账龄分析VO
 */
@Data
@Schema(description = "账龄分析")
public class AgingAnalysisVO {

    @Schema(description = "账龄区间")
    private String period;

    @Schema(description = "笔数")
    private Integer count;

    @Schema(description = "总金额")
    private BigDecimal totalAmount;

    @Schema(description = "占比")
    private BigDecimal percentage;
}
