package cn.aiedge.erp.sale.return.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaleReturnItemDTO {

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    private Long orderItemId;

    private BigDecimal orderQuantity;

    private BigDecimal returnQuantity;

    private BigDecimal unitPrice;

    private String batchNo;

    private String returnReason;

    private String remark;
}