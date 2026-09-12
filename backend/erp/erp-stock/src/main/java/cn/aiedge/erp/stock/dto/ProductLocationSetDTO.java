package cn.aiedge.erp.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 商品推荐货位设置 / 移除请求
 * <p>
 * 对标 ql361 setgoodsposition(goodssetpoint, goods[], gpid) 与 batchremove(goodssetpoint, goods[])：
 * 一次对「同一仓库」的若干商品设置或解除推荐货位。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "商品推荐货位设置请求")
public class ProductLocationSetDTO {

    @Schema(description = "仓库ID（必填）")
    private Long warehouseId;

    @Schema(description = "商品ID集合（必填）")
    private List<Long> productIds;

    @Schema(description = "货位ID（设置时必填，移除时忽略）")
    private Long locationId;

    @Schema(description = "备注")
    private String remark;
}
