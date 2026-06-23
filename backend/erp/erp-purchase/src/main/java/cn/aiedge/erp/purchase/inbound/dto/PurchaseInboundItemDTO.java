package cn.aiedge.erp.purchase.inbound.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseInboundItemDTO {

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    private Long orderItemId;

    private BigDecimal orderQuantity;

    private BigDecimal unitPrice;

    private BigDecimal unitCost;

    private BigDecimal taxRate;

    private String batchNo;

    private String remark;
}