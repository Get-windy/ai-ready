package cn.aiedge.erp.purchase.saledriven.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 以销定购-销售订单明细行（用于采购成品/采购原料的明细来源）
 * <p>
 * 数据源：erp_sale_order_item 表，按 order_id 查询。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class SalesDrivenItemDTO {

    /** 明细ID */
    private Long itemId;

    /** 商品ID */
    private Long productId;

    /** 商品编码 */
    private String productCode;

    /** 商品名称 */
    private String productName;

    /** 规格 */
    private String specification;

    /** 单位 */
    private String unit;

    /** 销售数量 */
    private BigDecimal quantity;

    /** 销售单价 */
    private BigDecimal unitPrice;

    /** 仓库ID */
    private Long warehouseId;
}
