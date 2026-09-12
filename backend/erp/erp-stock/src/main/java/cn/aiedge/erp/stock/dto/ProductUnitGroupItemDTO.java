package cn.aiedge.erp.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品单位组成员 DTO（对标表单一行：类型 | 单位名称 | 换算关系）
 */
@Data
@Schema(description = "商品单位组成员")
public class ProductUnitGroupItemDTO {

    @Schema(description = "引用的单位字典ID")
    private Long unitId;

    @Schema(description = "单位类型: SMALL小单位 MEDIUM中单位 LARGE大单位")
    private String unitType;

    @Schema(description = "单位名称快照")
    private String unitName;

    @Schema(description = "换算关系（相对于小单位的倍数，小单位为 1）")
    private BigDecimal conversionRate;

    @Schema(description = "组内排序")
    private Integer sortOrder;
}
