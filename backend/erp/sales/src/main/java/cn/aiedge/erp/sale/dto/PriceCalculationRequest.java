package cn.aiedge.erp.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * 价格计算请求DTO
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@Schema(description = "价格计算请求")
public class PriceCalculationRequest {

    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long customerId;

    @Schema(description = "客户等级")
    private String customerLevel;

    @Schema(description = "区域编码")
    private String regionCode;

    @Schema(description = "订单明细列表")
    private List<PriceCalculationItem> items;

    @Schema(description = "指定促销ID列表（可选）")
    private List<Long> promotionIds;

    /**
     * 价格计算明细项
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "价格计算明细项")
    public static class PriceCalculationItem {

        @Schema(description = "产品ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long productId;

        @Schema(description = "产品名称")
        private String productName;

        @Schema(description = "产品类别ID")
        private Long productCategoryId;

        @Schema(description = "基础单价")
        private BigDecimal basePrice;

        @Schema(description = "数量")
        private Integer quantity;
    }
}
