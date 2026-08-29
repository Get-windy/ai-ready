package cn.aiedge.erp.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 采购价格趋势点VO
 * <p>
 * 趋势弹窗：按采购日期升序返回该商品全部价格点，
 * 折线图展示价格变动，悬停显示"最近改价格成交供应商名称"。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "采购价格趋势点")
public class PurchasePriceTrendVO {

    @Schema(description = "采购日期")
    private LocalDate purchaseDate;

    @Schema(description = "采购价格")
    private BigDecimal purchasePrice;

    @Schema(description = "成交供应商（往来单位）名称")
    private String partnerName;

    @Schema(description = "该价格点记录ID")
    private Long id;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;
}
