package cn.aiedge.erp.sale.outbound.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaleOutboundItemDTO {

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    private Long orderItemId;

    private BigDecimal orderQuantity;

    private BigDecimal unitPrice;

    private String remark;
}