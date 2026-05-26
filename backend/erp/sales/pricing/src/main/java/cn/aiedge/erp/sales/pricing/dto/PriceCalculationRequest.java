package cn.aiedge.erp.sales.pricing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 价格计算请求DTO
 */
@Data
@Schema(description = "价格计算请求DTO")
public class PriceCalculationRequest {
    
    @NotNull(message = "租户ID不能为空")
    @Schema(description = "租户ID", required = true)
    private Long tenantId;
    
    @NotNull(message = "产品ID不能为空")
    @Schema(description = "产品ID", required = true)
    private Long productId;
    
    @Schema(description = "产品类别ID")
    private Long productCategoryId;
    
    @NotNull(message = "客户ID不能为空")
    @Schema(description = "客户ID", required = true)
    private Long customerId;
    
    @Schema(description = "客户等级")
    private String customerLevel;
    
    @Schema(description = "区域编码")
    private String regionCode;
    
    @NotNull(message = "数量不能为空")
    @Schema(description = "数量", required = true)
    private Integer quantity;
    
    @NotNull(message = "基础价格不能为空")
    @Schema(description = "基础价格", required = true)
    private BigDecimal basePrice;
    
    @Schema(description = "促销活动ID")
    private Long promotionId;
    
    @Schema(description = "销售渠道")
    private String salesChannel;
    
    @Schema(description = "订单类型")
    private String orderType;
    
    @Schema(description = "自定义扩展参数")
    private String customParams;
}