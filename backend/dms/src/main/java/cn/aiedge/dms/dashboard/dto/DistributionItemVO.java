package cn.aiedge.dms.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 分布统计项（渠道 / 订单类型）
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "配送任务分布统计项")
public class DistributionItemVO {

    @Schema(description = "维度键（渠道ID / 订单类型码）")
    private String itemKey;

    @Schema(description = "维度名称")
    private String itemName;

    @Schema(description = "单量")
    private Long orderCount;

    @Schema(description = "配送费合计")
    private BigDecimal amount;
}
