package cn.aiedge.erp.sales.pricing.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 价格策略DTO
 */
@Data
@Schema(description = "价格策略DTO")
public class PriceStrategyDTO {
    
    @Schema(description = "策略ID")
    private Long id;
    
    @NotBlank(message = "策略名称不能为空")
    @Schema(description = "策略名称", required = true)
    private String name;
    
    @Schema(description = "策略描述")
    private String description;
    
    @Schema(description = "策略类型")
    private String strategyType;
    
    @Schema(description = "适用客户等级")
    private String customerLevel;
    
    @Schema(description = "适用区域编码")
    private String regionCode;
    
    @Schema(description = "适用产品类别ID")
    private Long productCategoryId;
    
    @NotNull(message = "基础价格不能为空")
    @Schema(description = "基础价格", required = true)
    private BigDecimal basePrice;
    
    @Schema(description = "价格系数")
    private BigDecimal priceFactor;
    
    @Schema(description = "折扣率")
    private BigDecimal discountRate;
    
    @Schema(description = "折扣金额")
    private BigDecimal discountAmount;
    
    @Schema(description = "最小数量门槛")
    private Integer minQuantity;
    
    @Schema(description = "公式配置(JSON)")
    private String formulaConfig;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "生效开始时间")
    private LocalDateTime effectiveStartTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "生效结束时间")
    private LocalDateTime effectiveEndTime;
    
    @Schema(description = "优先级")
    private Integer priority = 0;
    
    @Schema(description = "状态")
    private String status = "draft";
}