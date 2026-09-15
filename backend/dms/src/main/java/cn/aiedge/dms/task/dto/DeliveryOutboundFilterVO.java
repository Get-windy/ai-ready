package cn.aiedge.dms.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 《发货查询》固定项筛选结果（配送状态 / 配送线路 → 命中的出库单号）
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "配送执行命中的出库单号集合")
public class DeliveryOutboundFilterVO {

    @Schema(description = "命中的来源单据号（销售出库单号）")
    private List<String> sourceBillNos;

    @Schema(description = "命中条数")
    private int matched;

    @Schema(description = "是否被上限截断（true=请收窄条件，避免过滤不完整）")
    private boolean truncated;
}
