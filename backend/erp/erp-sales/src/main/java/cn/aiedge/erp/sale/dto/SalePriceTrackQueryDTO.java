package cn.aiedge.erp.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 销售价格跟踪 查询参数DTO
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "销售价格跟踪查询参数")
public class SalePriceTrackQueryDTO {

    @Schema(description = "页码", defaultValue = "1")
    private Long current = 1L;

    @Schema(description = "每页大小", defaultValue = "20")
    private Long size = 20L;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "货号")
    private String productCode;

    @Schema(description = "条码")
    private String barcode;

    @Schema(description = "往来单位名称")
    private String partnerName;

    @Schema(description = "开始日期（最近销售日期）")
    private String startDate;

    @Schema(description = "结束日期（最近销售日期）")
    private String endDate;

    @Schema(description = "商品分类ID")
    private Long categoryId;

    @Schema(description = "商品单位")
    private String unitType;

    @Schema(description = "仅显示有折扣的记录（折扣 < 100）")
    private Boolean onlyDiscounted;

    @Schema(description = "仅显示有销售日期的记录")
    private Boolean onlyHasSale;
}
