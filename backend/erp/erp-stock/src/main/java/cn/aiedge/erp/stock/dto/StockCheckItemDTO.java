package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockCheckItemDTO {

    private Long productId;

    private String productCode;

    private String productName;

    private String batchNo;

    private BigDecimal bookQuantity;

    private BigDecimal actualQuantity;

    private BigDecimal diffQuantity;

    private BigDecimal unitCost;

    private String remark;
}