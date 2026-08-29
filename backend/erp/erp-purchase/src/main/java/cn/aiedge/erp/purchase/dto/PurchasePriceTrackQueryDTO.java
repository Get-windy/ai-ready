package cn.aiedge.erp.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 采购价格跟踪 查询参数DTO
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "采购价格跟踪查询参数")
public class PurchasePriceTrackQueryDTO {

    @Schema(description = "页码", defaultValue = "1")
    private Long current = 1L;

    @Schema(description = "每页大小", defaultValue = "20")
    private Long size = 20L;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "往来单位名称")
    private String partnerName;

    @Schema(description = "开始日期（最近采购日期）")
    private String startDate;

    @Schema(description = "结束日期（最近采购日期）")
    private String endDate;

    @Schema(description = "商品分类ID")
    private Long categoryId;

    @Schema(description = "单位类型（小单位/大单位，空为全部）")
    private String unitType;
}
