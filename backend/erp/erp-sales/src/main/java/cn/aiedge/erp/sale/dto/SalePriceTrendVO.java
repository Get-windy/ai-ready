package cn.aiedge.erp.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 销售价格趋势点VO
 * <p>
 * 趋势弹窗：按销售日期升序返回该商品全部价格点，
 * 折线图展示价格变动，悬停显示"最近改价成交客户名称"。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "销售价格趋势点")
public class SalePriceTrendVO {

    @Schema(description = "销售日期")
    private LocalDate saleDate;

    @Schema(description = "销售价")
    private BigDecimal salePrice;

    @Schema(description = "销售折扣（%）")
    private BigDecimal discountRate;

    @Schema(description = "成交客户（往来单位）名称")
    private String partnerName;

    @Schema(description = "该价格点记录ID")
    private Long id;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;
}
