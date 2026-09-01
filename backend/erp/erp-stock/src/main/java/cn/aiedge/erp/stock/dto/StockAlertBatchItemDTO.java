package cn.aiedge.erp.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 预警设置批量项（一次保存「商品×仓库」的一条上下限配置）。
 * <p>
 * 用于预警设置页的行内编辑保存与「批量设置」弹窗统一赋值：
 * 前端将预期结果展开为 items 列表后一次性提交，后端逐条 upsert（幂等）。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "预警设置批量项")
public class StockAlertBatchItemDTO {

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "库存上限")
    private BigDecimal maxStock;

    @Schema(description = "库存下限")
    private BigDecimal minStock;

    @Schema(description = "是否激活（默认true）")
    private Boolean active;
}
