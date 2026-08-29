package cn.aiedge.erp.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 采购价格跟踪 新增/修改参数DTO（价格折扣）
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "采购价格跟踪-价格折扣保存参数")
public class PurchasePriceTrackSaveDTO {

    @Schema(description = "价格记录ID（修改时传）")
    private Long id;

    @Schema(description = "商品ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品不能为空")
    private Long productId;

    @Schema(description = "商品编码")
    private String productCode;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "货号")
    private String itemCode;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "规格")
    private String specification;

    @Schema(description = "型号")
    private String model;

    @Schema(description = "产地")
    private String origin;

    @Schema(description = "往来单位ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "往来单位不能为空")
    private Long partnerId;

    @Schema(description = "往来单位编号")
    private String partnerCode;

    @Schema(description = "往来单位名称")
    private String partnerName;

    @Schema(description = "采购价格", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "采购价格不能为空")
    private BigDecimal purchasePrice;

    @Schema(description = "采购日期")
    private LocalDate purchaseDate;
}
