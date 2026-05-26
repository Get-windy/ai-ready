package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockAdjustDTO {

    private Long productId;

    private Long warehouseId;

    private BigDecimal quantity;

    private String batchNo;

    private String adjustType;

    private String remark;
}