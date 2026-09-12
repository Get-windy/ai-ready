package cn.aiedge.erp.stock.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 商品单位组行 VO
 *
 * <p>对标列表列：操作 | 单位 | 单位关系。
 * 「单位」= 单位名逗号串（袋,提,箱）；「单位关系」= 换算关系冒号串（1:12:48）。
 */
@Data
@Schema(description = "商品单位组行")
public class ProductUnitGroupVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private Integer status;

    @Schema(description = "单位名称聚合串（对标列「单位」，如 袋,提,箱）")
    private String unitNames;

    @Schema(description = "换算关系聚合串（对标列「单位关系」，如 1:12:48）")
    private String unitRates;

    @Schema(description = "单位明细（小单位/中单位/大单位）")
    private List<ProductUnitGroupItemDTO> items;
}
