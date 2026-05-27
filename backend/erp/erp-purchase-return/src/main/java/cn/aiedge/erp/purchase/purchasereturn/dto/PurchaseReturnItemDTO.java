package cn.aiedge.erp.purchase.purchasereturn.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseReturnItemDTO {

    private Long id;

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    private BigDecimal returnQuantity;

    private BigDecimal unitPrice;

    private String reason;

    private String remark;
}