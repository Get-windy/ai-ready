package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockTransferItemDTO {

    private Long productId;

    private String productCode;

    private String productName;

    private String batchNo;

    private BigDecimal quantity;

    private BigDecimal unitCost;

    private String remark;
}